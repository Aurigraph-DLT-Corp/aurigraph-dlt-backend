package io.aurigraph.v11.system;

import io.aurigraph.v11.system.models.SystemStatus;
import io.aurigraph.v11.system.models.SystemStatus.ConsensusStatus;
import io.aurigraph.v11.system.models.SystemStatus.HealthStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * System Status Repository - JPA/Panache Implementation
 *
 * Provides database persistence for SystemStatus entities.
 * Supports status queries, health monitoring, and performance analytics.
 *
 * @version 3.8.0 (Phase 2 Day 12)
 * @author Aurigraph V11 Development Team
 */
@ApplicationScoped
public class SystemStatusRepository implements PanacheRepository<SystemStatus> {

    // ==================== BASIC QUERIES ====================

    public Optional<SystemStatus> findLatestByNode(String nodeId) {
        return find("nodeId", Sort.descending("timestamp"), nodeId)
                .firstResultOptional();
    }

    public List<SystemStatus> findByNode(String nodeId, int limit) {
        return find("nodeId", Sort.descending("timestamp"), nodeId)
                .page(Page.ofSize(limit))
                .list();
    }

    public List<SystemStatus> findByNodeBetween(String nodeId, Instant start, Instant end) {
        return find("nodeId = ?1 and timestamp >= ?2 and timestamp <= ?3",
                Sort.ascending("timestamp"),
                nodeId, start, end)
                .list();
    }

    // ==================== HEALTH QUERIES ====================

    public List<SystemStatus> findByHealthStatus(HealthStatus status) {
        return find("healthStatus", Sort.descending("timestamp"), status).list();
    }

    public List<SystemStatus> findHealthyNodes() {
        return find("select s from SystemStatus s where s.healthStatus = ?1 " +
                        "and s.timestamp = (select max(s2.timestamp) from SystemStatus s2 where s2.nodeId = s.nodeId)",
                HealthStatus.HEALTHY)
                .list();
    }

    public List<SystemStatus> findUnhealthyNodes() {
        return find("select s from SystemStatus s where s.healthStatus in (?1, ?2, ?3) " +
                        "and s.timestamp = (select max(s2.timestamp) from SystemStatus s2 where s2.nodeId = s.nodeId)",
                HealthStatus.DEGRADED, HealthStatus.UNHEALTHY, HealthStatus.CRITICAL)
                .list();
    }

    public long countByHealthStatus(HealthStatus status) {
        return count("healthStatus", status);
    }

    // ==================== CONSENSUS QUERIES ====================

    public List<SystemStatus> findByConsensusStatus(ConsensusStatus status) {
        return find("consensusStatus", Sort.descending("timestamp"), status).list();
    }

    public List<SystemStatus> findSyncedNodes() {
        return find("select s from SystemStatus s where s.consensusStatus = ?1 " +
                        "and s.timestamp = (select max(s2.timestamp) from SystemStatus s2 where s2.nodeId = s.nodeId)",
                ConsensusStatus.SYNCED)
                .list();
    }

    public List<SystemStatus> findLeaderNodes() {
        return find("isLeader = true and consensusStatus = ?1",
                Sort.descending("timestamp"),
                ConsensusStatus.SYNCED)
                .list();
    }

    public Optional<SystemStatus> findCurrentLeader() {
        return find("isLeader = true and consensusStatus = ?1",
                Sort.descending("timestamp"),
                ConsensusStatus.SYNCED)
                .firstResultOptional();
    }

    // ==================== PERFORMANCE QUERIES ====================

    public List<SystemStatus> findByTpsRange(BigDecimal minTps, BigDecimal maxTps) {
        return find("tps >= ?1 and tps <= ?2",
                Sort.descending("tps"), minTps, maxTps)
                .list();
    }

    public List<SystemStatus> findTopPerformers(int limit) {
        return find("tps is not null and healthStatus = ?1",
                Sort.descending("tps"),
                HealthStatus.HEALTHY)
                .page(Page.ofSize(limit))
                .list();
    }

    public List<SystemStatus> findHighLatency(BigDecimal latencyThreshold) {
        return find("avgLatency > ?1",
                Sort.descending("avgLatency"), latencyThreshold)
                .list();
    }

    public Optional<SystemStatus> findPeakTps(String nodeId) {
        return find("nodeId = ?1 and peakTps is not null",
                Sort.descending("peakTps"), nodeId)
                .firstResultOptional();
    }

    // ==================== RESOURCE USAGE QUERIES ====================

    public List<SystemStatus> findHighCpuUsage(BigDecimal cpuThreshold) {
        return find("cpuUsage > ?1",
                Sort.descending("cpuUsage"), cpuThreshold)
                .list();
    }

    public List<SystemStatus> findHighMemoryUsage(int percentThreshold) {
        return find("(memoryUsed * 100.0 / memoryTotal) > ?1",
                Sort.descending("memoryUsed"), percentThreshold)
                .list();
    }

    public List<SystemStatus> findHighDiskUsage(int percentThreshold) {
        return find("(diskUsed * 100.0 / diskTotal) > ?1",
                Sort.descending("diskUsed"), percentThreshold)
                .list();
    }

    public List<SystemStatus> findResourceConstrained() {
        return find("cpuUsage > 80 or (memoryUsed * 100.0 / memoryTotal) > 80 or (diskUsed * 100.0 / diskTotal) > 80",
                Sort.descending("timestamp"))
                .list();
    }

    // ==================== SERVICE AVAILABILITY QUERIES ====================

    public List<SystemStatus> findApiUnavailable() {
        return find("apiAvailable = false", Sort.descending("timestamp")).list();
    }

    public List<SystemStatus> findGrpcUnavailable() {
        return find("grpcAvailable = false", Sort.descending("timestamp")).list();
    }

    public List<SystemStatus> findDatabaseUnavailable() {
        return find("databaseAvailable = false", Sort.descending("timestamp")).list();
    }

    public List<SystemStatus> findAnyServiceUnavailable() {
        return find("apiAvailable = false or grpcAvailable = false or databaseAvailable = false or cacheAvailable = false",
                Sort.descending("timestamp"))
                .list();
    }

    // ==================== ERROR TRACKING QUERIES ====================

    public List<SystemStatus> findRecentErrors(long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("lastErrorAt > ?1", Sort.descending("lastErrorAt"), since).list();
    }

    public List<SystemStatus> findHighErrorRate(long errorThreshold) {
        return find("errorCount > ?1", Sort.descending("errorCount"), errorThreshold).list();
    }

    public List<SystemStatus> findNodesWithErrors() {
        return find("errorCount > 0", Sort.descending("errorCount")).list();
    }

    // ==================== NETWORK QUERIES ====================

    public List<SystemStatus> findHighNetworkErrors(long errorThreshold) {
        return find("networkErrors > ?1",
                Sort.descending("networkErrors"), errorThreshold)
                .list();
    }

    public List<SystemStatus> findLowPeerCount(int peerThreshold) {
        return find("peerCount < ?1 and consensusStatus = ?2",
                Sort.ascending("peerCount"),
                peerThreshold,
                ConsensusStatus.SYNCED)
                .list();
    }

    public List<SystemStatus> findIsolatedNodes() {
        return find("activePeers = 0 or connectionCount = 0",
                Sort.descending("timestamp"))
                .list();
    }

    // ==================== TIME-BASED QUERIES ====================

    public List<SystemStatus> findRecent(long secondsAgo) {
        Instant since = Instant.now().minusSeconds(secondsAgo);
        return find("timestamp > ?1", Sort.descending("timestamp"), since).list();
    }

    public List<SystemStatus> findBetween(Instant start, Instant end) {
        return find("timestamp >= ?1 and timestamp <= ?2",
                Sort.ascending("timestamp"), start, end)
                .list();
    }

    public List<SystemStatus> findLongRunningNodes(long uptimeSeconds) {
        return find("uptime > ?1", Sort.descending("uptime"), uptimeSeconds).list();
    }

    // ==================== BLOCK HEIGHT QUERIES ====================

    public Optional<SystemStatus> findMaxBlockHeight() {
        return find("blockHeight is not null", Sort.descending("blockHeight"))
                .firstResultOptional();
    }

    public List<SystemStatus> findBehindNodes(long blockHeightThreshold) {
        Optional<SystemStatus> maxHeight = findMaxBlockHeight();
        if (maxHeight.isEmpty()) {
            return List.of();
        }

        long max = maxHeight.get().getBlockHeight();
        return find("blockHeight < ?1", Sort.ascending("blockHeight"), max - blockHeightThreshold)
                .list();
    }

    // ==================== STATISTICS ====================

    public SystemHealthStatistics getHealthStatistics() {
        long total = count();
        long healthy = countByHealthStatus(HealthStatus.HEALTHY);
        long degraded = countByHealthStatus(HealthStatus.DEGRADED);
        long unhealthy = countByHealthStatus(HealthStatus.UNHEALTHY);
        long critical = countByHealthStatus(HealthStatus.CRITICAL);
        long unknown = countByHealthStatus(HealthStatus.UNKNOWN);

        // Latest status per node
        List<SystemStatus> latestStatuses = find("select s from SystemStatus s " +
                "where s.timestamp = (select max(s2.timestamp) from SystemStatus s2 where s2.nodeId = s.nodeId)")
                .list();

        long activeNodes = latestStatuses.size();
        long healthyNodes = latestStatuses.stream()
                .filter(s -> s.getHealthStatus() == HealthStatus.HEALTHY)
                .count();

        BigDecimal avgTps = latestStatuses.stream()
                .map(SystemStatus::getTps)
                .filter(tps -> tps != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, latestStatuses.size())), 2, BigDecimal.ROUND_HALF_UP);

        return new SystemHealthStatistics(
                total,
                activeNodes,
                healthyNodes,
                healthy,
                degraded,
                unhealthy,
                critical,
                unknown,
                avgTps
        );
    }

    public NodePerformanceStatistics getNodeStatistics(String nodeId, Instant start, Instant end) {
        List<SystemStatus> statuses = findByNodeBetween(nodeId, start, end);

        if (statuses.isEmpty()) {
            return new NodePerformanceStatistics(nodeId, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L);
        }

        int count = statuses.size();
        BigDecimal avgTps = statuses.stream()
                .map(SystemStatus::getTps)
                .filter(tps -> tps != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);

        BigDecimal maxTps = statuses.stream()
                .map(SystemStatus::getTps)
                .filter(tps -> tps != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal avgLatency = statuses.stream()
                .map(SystemStatus::getAvgLatency)
                .filter(lat -> lat != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);

        BigDecimal avgCpu = statuses.stream()
                .map(SystemStatus::getCpuUsage)
                .filter(cpu -> cpu != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);

        long totalTx = statuses.stream()
                .mapToLong(SystemStatus::getTotalTransactions)
                .sum();

        long failedTx = statuses.stream()
                .mapToLong(SystemStatus::getFailedTransactions)
                .sum();

        long errors = statuses.stream()
                .mapToLong(SystemStatus::getErrorCount)
                .sum();

        return new NodePerformanceStatistics(
                nodeId, count, avgTps, maxTps, avgLatency, avgCpu,
                totalTx, failedTx, errors
        );
    }

    // ==================== CLEANUP ====================

    public long deleteBefore(Instant before) {
        return delete("timestamp < ?1", before);
    }

    public long deleteByNode(String nodeId) {
        return delete("nodeId", nodeId);
    }

    // ==================== DATA MODELS ====================

    public record SystemHealthStatistics(
            long totalRecords,
            long activeNodes,
            long healthyNodes,
            long healthyRecords,
            long degradedRecords,
            long unhealthyRecords,
            long criticalRecords,
            long unknownRecords,
            BigDecimal avgTps
    ) {}

    public record NodePerformanceStatistics(
            String nodeId,
            int recordCount,
            BigDecimal avgTps,
            BigDecimal maxTps,
            BigDecimal avgLatency,
            BigDecimal avgCpuUsage,
            long totalTransactions,
            long failedTransactions,
            long errorCount
    ) {}
}
