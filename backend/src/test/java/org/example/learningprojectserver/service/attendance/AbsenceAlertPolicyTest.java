package org.example.learningprojectserver.service.attendance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbsenceAlertPolicyTest {

    @Test
    void studentAlertsOnEveryThirdAbsence() {
        assertFalse(AbsenceAlertPolicy.studentAlert(1));
        assertFalse(AbsenceAlertPolicy.studentAlert(2));
        assertTrue(AbsenceAlertPolicy.studentAlert(3));
        assertFalse(AbsenceAlertPolicy.studentAlert(4));
        assertTrue(AbsenceAlertPolicy.studentAlert(6));
    }

    @Test
    void classAlertsOnlyWhenCrossingHalf() {
        assertTrue(AbsenceAlertPolicy.classAlert(1, 5, 10));
        assertFalse(AbsenceAlertPolicy.classAlert(5, 6, 10), "already over the line");
        assertFalse(AbsenceAlertPolicy.classAlert(0, 4, 10));
        assertFalse(AbsenceAlertPolicy.classAlert(0, 3, 3), "class too small");
    }

    @Test
    void reportFlagsByCountOrRate() {
        assertTrue(AbsenceAlertPolicy.unusualInReport(3, 40));
        assertTrue(AbsenceAlertPolicy.unusualInReport(2, 10));
        assertFalse(AbsenceAlertPolicy.unusualInReport(2, 11));
        assertFalse(AbsenceAlertPolicy.unusualInReport(1, 4));
    }
}
