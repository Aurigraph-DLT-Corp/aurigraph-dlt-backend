/**
 * Aurigraph V11 Real-Time Performance Monitoring
 * High-performance JavaScript monitoring system for 2M+ TPS tracking
 */

class AurigraphMonitor {
    constructor() {
        this.tpsChart = null;
        this.resourceChart = null;
        this.metricsHistory = {
            tps: [],
            memory: [],
            cpu: [],
            timestamps: []
        };
        this.maxDataPoints = 50;
        this.isInitialized = false;
        this.websocket = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        
        console.log('Aurigraph Monitor initialized');
    }

    /**
     * Initialize charts and start monitoring
     */
    initialize() {
        if (this.isInitialized) return;
        
        try {
            this.initializeCharts();
            this.startPerformanceMonitoring();
            this.initializeWebSocket();
            this.isInitialized = true;
            console.log('Aurigraph Monitor fully initialized');
        } catch (error) {
            console.error('Monitor initialization failed:', error);
        }
    }

    /**
     * Initialize Chart.js charts
     */
    initializeCharts() {
        const chartConfig = {
            responsive: true,
            maintainAspectRatio: false,
            animation: {
                duration: 750,
                easing: 'easeInOutQuart'
            },
            interaction: {
                intersect: false,
                mode: 'index'
            },
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        usePointStyle: true,
                        padding: 20
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    titleColor: 'white',
                    bodyColor: 'white',
                    borderColor: '#667eea',
                    borderWidth: 1
                }
            },
            scales: {
                x: {
                    type: 'time',
                    time: {
                        unit: 'second',
                        displayFormats: {
                            second: 'HH:mm:ss'
                        }
                    },
                    title: {
                        display: true,
                        text: 'Time'
                    }
                },
                y: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: 'Value'
                    }
                }
            }
        };

        // TPS Performance Chart
        const tpsCtx = document.getElementById('tpsChart');
        if (tpsCtx) {
            this.tpsChart = new Chart(tpsCtx, {
                type: 'line',
                data: {
                    datasets: [
                        {
                            label: 'Current TPS',
                            data: [],
                            borderColor: '#667eea',
                            backgroundColor: 'rgba(102, 126, 234, 0.1)',
                            fill: true,
                            tension: 0.4,
                            pointRadius: 3,
                            pointHoverRadius: 6
                        },
                        {
                            label: 'Target TPS (2M)',
                            data: [],
                            borderColor: '#10b981',
                            backgroundColor: 'rgba(16, 185, 129, 0.1)',
                            borderDash: [5, 5],
                            fill: false,
                            tension: 0
                        }
                    ]
                },
                options: {
                    ...chartConfig,
                    scales: {
                        ...chartConfig.scales,
                        y: {
                            ...chartConfig.scales.y,
                            title: {
                                display: true,
                                text: 'Transactions Per Second'
                            },
                            suggestedMax: 2500000
                        }
                    }
                }
            });
        }

        // Resource Usage Chart
        const resourceCtx = document.getElementById('resourceChart');
        if (resourceCtx) {
            this.resourceChart = new Chart(resourceCtx, {
                type: 'line',
                data: {
                    datasets: [
                        {
                            label: 'Memory Usage (%)',
                            data: [],
                            borderColor: '#f59e0b',
                            backgroundColor: 'rgba(245, 158, 11, 0.1)',
                            fill: true,
                            tension: 0.4,
                            yAxisID: 'y'
                        },
                        {
                            label: 'CPU Usage (%)',
                            data: [],
                            borderColor: '#ef4444',
                            backgroundColor: 'rgba(239, 68, 68, 0.1)',
                            fill: true,
                            tension: 0.4,
                            yAxisID: 'y'
                        },
                        {
                            label: 'Network I/O (MB/s)',
                            data: [],
                            borderColor: '#8b5cf6',
                            backgroundColor: 'rgba(139, 92, 246, 0.1)',
                            fill: false,
                            tension: 0.4,
                            yAxisID: 'y1'
                        }
                    ]
                },
                options: {
                    ...chartConfig,
                    scales: {
                        ...chartConfig.scales,
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            beginAtZero: true,
                            max: 100,
                            title: {
                                display: true,
                                text: 'Percentage (%)'
                            }
                        },
                        y1: {
                            type: 'linear',
                            display: true,
                            position: 'right',
                            beginAtZero: true,
                            title: {
                                display: true,
                                text: 'Network (MB/s)'
                            },
                            grid: {
                                drawOnChartArea: false,
                            },
                        }
                    }
                }
            });
        }

        console.log('Charts initialized successfully');
    }

    /**
     * Start performance monitoring loop
     */
    startPerformanceMonitoring() {
        // Initial data fetch
        this.fetchAndUpdateMetrics();
        
        // Set up periodic updates
        setInterval(() => {
            this.fetchAndUpdateMetrics();
        }, 2000); // Update every 2 seconds

        // High-frequency TPS monitoring
        setInterval(() => {
            this.updateTPSMetrics();
        }, 500); // Update TPS every 500ms for real-time feel

        console.log('Performance monitoring started');
    }

    /**
     * Fetch metrics from API and update charts
     */
    async fetchAndUpdateMetrics() {
        try {
            const timestamp = new Date();
            
            // Fetch performance metrics
            const performanceResponse = await fetch('/api/v11/performance/metrics');
            if (performanceResponse.ok) {
                const performanceData = await performanceResponse.json();
                this.updatePerformanceCharts(performanceData, timestamp);
            }

            // Fetch system information
            const systemResponse = await fetch('/api/v11/info');
            if (systemResponse.ok) {
                const systemData = await systemResponse.json();
                this.updateSystemMetrics(systemData, timestamp);
            }

            // Simulate additional metrics (in production, these would come from actual monitoring)
            this.simulateSystemResources(timestamp);

        } catch (error) {
            console.error('Failed to fetch metrics:', error);
            this.handleMetricsError(error);
        }
    }

    /**
     * Update TPS metrics with high frequency
     */
    async updateTPSMetrics() {
        try {
            const response = await fetch('/api/v11/stats');
            if (response.ok) {
                const stats = await response.json();
                const timestamp = new Date();
                
                // Add current TPS data point
                if (this.tpsChart && stats.currentThroughputMeasurement !== undefined) {
                    this.addDataPoint(
                        this.tpsChart.data.datasets[0],
                        timestamp,
                        stats.currentThroughputMeasurement
                    );
                    
                    // Add target line
                    this.addDataPoint(
                        this.tpsChart.data.datasets[1],
                        timestamp,
                        2000000 // 2M TPS target
                    );
                    
                    this.tpsChart.update('none'); // No animation for high-frequency updates
                }
            }
        } catch (error) {
            console.debug('TPS metrics update failed:', error);
        }
    }

    /**
     * Update performance charts with new data
     */
    updatePerformanceCharts(data, timestamp) {
        const currentTPS = data.currentTPS || 0;
        
        // Store in history
        this.metricsHistory.tps.push(currentTPS);
        this.metricsHistory.timestamps.push(timestamp);
        
        // Maintain max data points
        if (this.metricsHistory.tps.length > this.maxDataPoints) {
            this.metricsHistory.tps.shift();
            this.metricsHistory.timestamps.shift();
        }

        // Update TPS chart if not using high-frequency updates
        if (this.tpsChart && !this.isHighFrequencyMode) {
            this.addDataPoint(this.tpsChart.data.datasets[0], timestamp, currentTPS);
            this.addDataPoint(this.tpsChart.data.datasets[1], timestamp, 2000000);
            this.tpsChart.update();
        }

        // Update performance indicators on the page
        this.updatePerformanceIndicators(data);
    }

    /**
     * Update system metrics
     */
    updateSystemMetrics(data, timestamp) {
        // Calculate memory usage percentage
        const memoryUsage = data.maxMemoryMB ? 
            ((data.maxMemoryMB - (data.maxMemoryMB * 0.3)) / data.maxMemoryMB) * 100 : 
            Math.random() * 30 + 50; // Simulate 50-80% usage

        this.metricsHistory.memory.push(memoryUsage);
        
        if (this.metricsHistory.memory.length > this.maxDataPoints) {
            this.metricsHistory.memory.shift();
        }
    }

    /**
     * Simulate system resource metrics (for demonstration)
     */
    simulateSystemResources(timestamp) {
        // Simulate CPU usage based on TPS load
        const currentTPS = this.metricsHistory.tps[this.metricsHistory.tps.length - 1] || 0;
        const cpuUsage = Math.min(95, (currentTPS / 2000000) * 80 + Math.random() * 10 + 10);
        
        // Simulate network I/O
        const networkIO = (currentTPS / 50000) + Math.random() * 5;
        
        // Simulate memory usage fluctuation
        const memoryUsage = 60 + Math.sin(Date.now() / 10000) * 15 + Math.random() * 5;

        this.metricsHistory.cpu.push(cpuUsage);
        this.metricsHistory.memory.push(memoryUsage);

        // Maintain history size
        if (this.metricsHistory.cpu.length > this.maxDataPoints) {
            this.metricsHistory.cpu.shift();
            this.metricsHistory.memory.shift();
        }

        // Update resource chart
        if (this.resourceChart) {
            this.addDataPoint(this.resourceChart.data.datasets[0], timestamp, memoryUsage);
            this.addDataPoint(this.resourceChart.data.datasets[1], timestamp, cpuUsage);
            this.addDataPoint(this.resourceChart.data.datasets[2], timestamp, networkIO);
            this.resourceChart.update();
        }
    }

    /**
     * Add data point to chart dataset
     */
    addDataPoint(dataset, timestamp, value) {
        dataset.data.push({
            x: timestamp,
            y: value
        });

        // Remove old data points
        if (dataset.data.length > this.maxDataPoints) {
            dataset.data.shift();
        }
    }

    /**
     * Update performance indicators on the dashboard
     */
    updatePerformanceIndicators(data) {
        // Update any performance indicators in the DOM
        const tpsElement = document.querySelector('[data-metric="current-tps"]');
        if (tpsElement) {
            tpsElement.textContent = this.formatNumber(Math.round(data.currentTPS || 0));
        }

        const efficiencyElement = document.querySelector('[data-metric="efficiency"]');
        if (efficiencyElement) {
            efficiencyElement.textContent = `${(data.throughputEfficiency * 100 || 0).toFixed(1)}%`;
        }
    }

    /**
     * Initialize WebSocket connection for real-time updates
     */
    initializeWebSocket() {
        try {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = `${protocol}//${window.location.host}/ws/metrics`;
            
            this.websocket = new WebSocket(wsUrl);
            
            this.websocket.onopen = () => {
                console.log('WebSocket connected for real-time metrics');
                this.reconnectAttempts = 0;
            };
            
            this.websocket.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    this.handleWebSocketData(data);
                } catch (error) {
                    console.error('WebSocket data parsing error:', error);
                }
            };
            
            this.websocket.onclose = () => {
                console.log('WebSocket connection closed');
                this.attemptReconnect();
            };
            
            this.websocket.onerror = (error) => {
                console.error('WebSocket error:', error);
            };
            
        } catch (error) {
            console.warn('WebSocket initialization failed, falling back to HTTP polling:', error);
        }
    }

    /**
     * Handle WebSocket data
     */
    handleWebSocketData(data) {
        const timestamp = new Date();
        
        switch (data.type) {
            case 'tps_update':
                if (this.tpsChart) {
                    this.addDataPoint(this.tpsChart.data.datasets[0], timestamp, data.value);
                    this.tpsChart.update('none');
                }
                break;
                
            case 'system_metrics':
                if (this.resourceChart) {
                    this.addDataPoint(this.resourceChart.data.datasets[0], timestamp, data.memory);
                    this.addDataPoint(this.resourceChart.data.datasets[1], timestamp, data.cpu);
                    this.addDataPoint(this.resourceChart.data.datasets[2], timestamp, data.network);
                    this.resourceChart.update('none');
                }
                break;
                
            default:
                console.debug('Unknown WebSocket data type:', data.type);
        }
    }

    /**
     * Attempt WebSocket reconnection
     */
    attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`Attempting WebSocket reconnection (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
            
            setTimeout(() => {
                this.initializeWebSocket();
            }, 2000 * this.reconnectAttempts); // Exponential backoff
        } else {
            console.warn('Max WebSocket reconnection attempts reached, continuing with HTTP polling');
        }
    }

    /**
     * Handle metrics fetch errors
     */
    handleMetricsError(error) {
        console.error('Metrics error:', error);
        
        // Could update UI to show error state
        const errorIndicator = document.querySelector('[data-indicator="connection-status"]');
        if (errorIndicator) {
            errorIndicator.textContent = 'Connection Error';
            errorIndicator.className = 'status-critical';
        }
    }

    /**
     * Format numbers for display
     */
    formatNumber(num) {
        if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
        if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
        return num.toLocaleString();
    }

    /**
     * Export metrics data
     */
    exportMetrics() {
        const exportData = {
            timestamp: new Date().toISOString(),
            metrics: this.metricsHistory,
            summary: {
                avgTPS: this.metricsHistory.tps.reduce((a, b) => a + b, 0) / this.metricsHistory.tps.length,
                maxTPS: Math.max(...this.metricsHistory.tps),
                avgMemory: this.metricsHistory.memory.reduce((a, b) => a + b, 0) / this.metricsHistory.memory.length,
                avgCPU: this.metricsHistory.cpu.reduce((a, b) => a + b, 0) / this.metricsHistory.cpu.length
            }
        };

        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `aurigraph-metrics-${Date.now()}.json`;
        a.click();
        URL.revokeObjectURL(url);
    }

    /**
     * Cleanup resources
     */
    destroy() {
        if (this.websocket) {
            this.websocket.close();
        }
        
        if (this.tpsChart) {
            this.tpsChart.destroy();
        }
        
        if (this.resourceChart) {
            this.resourceChart.destroy();
        }
        
        console.log('Aurigraph Monitor destroyed');
    }
}

// Global monitor instance
let aurigraphMonitor = null;

// Initialize monitoring when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    aurigraphMonitor = new AurigraphMonitor();
});

// Global function to initialize charts (called from React component)
window.initializeCharts = () => {
    if (aurigraphMonitor && !aurigraphMonitor.isInitialized) {
        aurigraphMonitor.initialize();
    }
};

// Export for external use
window.AurigraphMonitor = AurigraphMonitor;
window.aurigraphMonitor = aurigraphMonitor;

// Performance testing utilities
window.performanceTest = {
    /**
     * Run a quick performance test
     */
    async runQuickTest(iterations = 10000) {
        try {
            const response = await fetch('/api/v11/performance/test', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    iterations: iterations,
                    threadCount: 8
                })
            });
            
            if (response.ok) {
                const result = await response.json();
                console.log('Performance Test Result:', result);
                return result;
            } else {
                throw new Error(`Test failed: ${response.status}`);
            }
        } catch (error) {
            console.error('Performance test error:', error);
            throw error;
        }
    },

    /**
     * Run ultra-high throughput test
     */
    async runUltraTest(iterations = 100000) {
        try {
            const response = await fetch('/api/v11/performance/ultra-throughput', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    iterations: iterations
                })
            });
            
            if (response.ok) {
                const result = await response.json();
                console.log('Ultra-High Throughput Test Result:', result);
                return result;
            } else {
                throw new Error(`Ultra test failed: ${response.status}`);
            }
        } catch (error) {
            console.error('Ultra throughput test error:', error);
            throw error;
        }
    }
};

console.log('Aurigraph V11 Monitoring System Loaded');
console.log('Available functions: window.performanceTest.runQuickTest(), window.performanceTest.runUltraTest()');
console.log('Monitor instance: window.aurigraphMonitor');