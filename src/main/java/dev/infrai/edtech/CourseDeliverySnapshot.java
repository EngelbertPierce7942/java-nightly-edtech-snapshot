package dev.infrai.edtech;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CourseDeliverySnapshot(
        LocalDate businessDate,
        Instant capturedAt,
        List<CourseDelivery> courseDeliveries,
        List<LearnerDeadline> learnerDeadlines,
        EducatorReport educatorReport) {

    public record CourseDelivery(String courseId, String cohortId, String state) {}
    public record LearnerDeadline(String learnerId, String courseId, LocalDate dueDate, String status) {}
    public record EducatorReport(String educatorId, int activeCourses, int overdueLearners) {}

    public Map<String, Object> asJson() {
        return Map.of(
                "business_date", businessDate.toString(),
                "captured_at", capturedAt.toString(),
                "course_deliveries", courseDeliveries.stream().map(item -> Map.of(
                        "course_id", item.courseId(), "cohort_id", item.cohortId(), "state", item.state())).toList(),
                "learner_deadlines", learnerDeadlines.stream().map(item -> Map.of(
                        "learner_id", item.learnerId(), "course_id", item.courseId(),
                        "due_date", item.dueDate().toString(), "status", item.status())).toList(),
                "educator_report", Map.of(
                        "educator_id", educatorReport.educatorId(),
                        "active_courses", educatorReport.activeCourses(),
                        "overdue_learners", educatorReport.overdueLearners()));
    }
}
