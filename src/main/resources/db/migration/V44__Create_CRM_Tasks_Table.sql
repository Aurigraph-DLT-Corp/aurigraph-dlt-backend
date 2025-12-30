-- V44: Create CRM Tasks Table
-- Purpose: Track follow-up tasks, action items, and team responsibilities
-- Created: December 27, 2025

-- Create ENUM types
CREATE TYPE task_status AS ENUM ('open', 'in_progress', 'completed', 'cancelled');
CREATE TYPE task_priority AS ENUM ('low', 'normal', 'high', 'urgent');
CREATE TYPE task_category AS ENUM ('follow_up', 'call', 'email', 'meeting', 'proposal', 'contract', 'demo_prep', 'internal_review', 'other');

-- Create tasks table
CREATE TABLE IF NOT EXISTS tasks (
    -- Primary Key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign Keys
    lead_id UUID NOT NULL,
    opportunity_id UUID,
    related_interaction_id UUID,
    assigned_to_user_id UUID NOT NULL,
    created_by_user_id UUID,

    -- Task Details
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category task_category DEFAULT 'follow_up' NOT NULL,
    status task_status DEFAULT 'open' NOT NULL,
    priority task_priority DEFAULT 'normal' NOT NULL,

    -- Timing
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    due_date DATE NOT NULL,
    reminder_date DATE,
    reminder_sent BOOLEAN DEFAULT FALSE,
    reminder_sent_at TIMESTAMP WITH TIME ZONE,

    -- Completion
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    days_to_complete INTEGER,  -- Calculated: completed_at - created_at

    -- Dependencies
    parent_task_id UUID,  -- For sub-tasks
    blocking_task_id UUID,  -- Task that blocks this one

    -- Outcome & Notes
    completion_notes TEXT,
    outcome_summary VARCHAR(500),
    success BOOLEAN,  -- Did this task achieve its goal?
    next_steps TEXT,

    -- SLA & Metrics
    is_sla_task BOOLEAN DEFAULT FALSE,
    sla_due_at TIMESTAMP WITH TIME ZONE,
    sla_breached BOOLEAN DEFAULT FALSE,

    -- Audit Fields
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by UUID,

    -- Constraints
    CONSTRAINT tasks_lead_fk FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT tasks_opportunity_fk FOREIGN KEY (opportunity_id) REFERENCES opportunities(id) ON DELETE SET NULL,
    CONSTRAINT tasks_interaction_fk FOREIGN KEY (related_interaction_id) REFERENCES interactions(id) ON DELETE SET NULL,
    CONSTRAINT tasks_assigned_to_fk FOREIGN KEY (assigned_to_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT tasks_created_by_fk FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT tasks_parent_task_fk FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    CONSTRAINT tasks_blocking_task_fk FOREIGN KEY (blocking_task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    CONSTRAINT tasks_due_date_logic CHECK (completed_at IS NULL OR completed_at >= created_at)
);

-- Create indexes for common queries
CREATE INDEX idx_tasks_lead_id ON tasks(lead_id);
CREATE INDEX idx_tasks_assigned_to ON tasks(assigned_to_user_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
CREATE INDEX idx_tasks_category ON tasks(category);
CREATE INDEX idx_tasks_opportunity_id ON tasks(opportunity_id);
CREATE INDEX idx_tasks_status_assigned ON tasks(status, assigned_to_user_id);
CREATE INDEX idx_tasks_status_due_priority ON tasks(status, due_date, priority) WHERE status != 'completed';
CREATE INDEX idx_tasks_sla_breached ON tasks(sla_breached) WHERE sla_breached = TRUE;
CREATE INDEX idx_tasks_parent_id ON tasks(parent_task_id) WHERE parent_task_id IS NOT NULL;

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_tasks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    IF NEW.status = 'completed' AND OLD.status != 'completed' THEN
        NEW.completed_at = COALESCE(NEW.completed_at, CURRENT_TIMESTAMP);
        NEW.days_to_complete = EXTRACT(DAY FROM (NEW.completed_at - NEW.created_at))::INTEGER;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tasks_update_timestamp
BEFORE UPDATE ON tasks
FOR EACH ROW
EXECUTE FUNCTION update_tasks_updated_at();

-- Create function to get overdue tasks
CREATE OR REPLACE FUNCTION get_overdue_tasks(p_user_id UUID DEFAULT NULL)
RETURNS TABLE (
    id UUID,
    title VARCHAR,
    lead_id UUID,
    assigned_to_user_id UUID,
    due_date DATE,
    priority task_priority,
    days_overdue INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        t.id,
        t.title,
        t.lead_id,
        t.assigned_to_user_id,
        t.due_date,
        t.priority,
        (CURRENT_DATE - t.due_date)::INTEGER as days_overdue
    FROM tasks t
    WHERE t.status != 'completed'
        AND t.due_date < CURRENT_DATE
        AND (p_user_id IS NULL OR t.assigned_to_user_id = p_user_id)
    ORDER BY t.due_date ASC, t.priority DESC;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON TABLE tasks IS 'Follow-up tasks and action items assigned to team members for each lead and opportunity.';
COMMENT ON COLUMN tasks.status IS 'Task lifecycle: open → in_progress → completed or cancelled';
COMMENT ON COLUMN tasks.priority IS 'Task priority: low, normal, high, urgent - affects task ordering and SLA.';
COMMENT ON COLUMN tasks.days_to_complete IS 'Number of days taken to complete task (calculated when status moves to completed).';
COMMENT ON COLUMN tasks.is_sla_task IS 'Whether this task is covered by SLA (e.g., first response to inquiry must be within 24 hours).';
COMMENT ON COLUMN tasks.sla_breached IS 'True if sla_due_at has passed and task is not completed.';
