package com.ssafy.alpaca.api.service;

import com.ssafy.alpaca.api.request.ScheduleListReq;
import com.ssafy.alpaca.api.request.ScheduleUpdateReq;
import com.ssafy.alpaca.api.request.ScheduleReq;
import com.ssafy.alpaca.api.response.ScheduleRes;
import com.ssafy.alpaca.api.response.ScheduleListRes;
import com.ssafy.alpaca.common.util.ExceptionUtil;
import com.ssafy.alpaca.db.document.Code;
import com.ssafy.alpaca.db.document.Problem;
import com.ssafy.alpaca.db.entity.Schedule;
import com.ssafy.alpaca.db.entity.Study;
import com.ssafy.alpaca.db.entity.ToSolveProblem;
import com.ssafy.alpaca.db.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final StudyRepository studyRepository;
    private final ScheduleRepository scheduleRepository;
    private final ProblemRepository problemRepository;
    private final ToSolveProblemRepository toSolveProblemRepository;
    private final CodeRepository codeRepository;

    private Map<String, String> getMessage(String returnMessage){
        Map<String, String> map = new HashMap<>();
        map.put("message", returnMessage);
        return map;
    }

    public String createSchedule(ScheduleReq scheduleReq) throws IllegalAccessException {
        if (scheduleReq.getFinishedAt().isAfter(scheduleReq.getStartedAt()) ||
        scheduleReq.getFinishedAt().isEqual(scheduleReq.getStartedAt())) {
            throw new IllegalAccessException(ExceptionUtil.INVALID_DATE_VALUE);
        }

        Study study = studyRepository.findById(scheduleReq.getStudyId()).orElseThrow(
                () -> new NoSuchElementException(ExceptionUtil.STUDY_NOT_FOUND)
        );

        if (scheduleRepository.existsByStudyAndStartedAtDate(
                study, LocalDate.of(
                        scheduleReq.getStartedAt().getYear(),
                        scheduleReq.getStartedAt().getMonth(),
                        scheduleReq.getStartedAt().getDayOfMonth())
        )) {
            throw new DuplicateFormatFlagsException(ExceptionUtil.STUDY_DATE_DUPLICATE);
        }

        Schedule schedule = scheduleRepository.save(
                Schedule.builder()
                        .study(study)
                        .startedAt(scheduleReq.getStartedAt())
                        .finishedAt(scheduleReq.getFinishedAt())
                        .build());
        for(String id:scheduleReq.getToSolveProblems())
        {
                ToSolveProblem toSolveProblem = toSolveProblemRepository.save(
                        ToSolveProblem.builder()
                                .isSolved(false)
                                .schedule(schedule)
                                .problemId(id)
                                .build());
        };

        return String.valueOf(schedule.getId());
    }

    public void updateSchedule(Long id, ScheduleUpdateReq scheduleUpdateReq) throws IllegalAccessException {
        if (scheduleUpdateReq.getFinishedAt().isAfter(scheduleUpdateReq.getStartedAt()) ||
                scheduleUpdateReq.getFinishedAt().isEqual(scheduleUpdateReq.getStartedAt())) {
            throw new IllegalAccessException(ExceptionUtil.INVALID_DATE_VALUE);
        }

        Schedule schedule = scheduleRepository.findById(id).orElseThrow(() -> new NoSuchElementException(ExceptionUtil.SCHEDULE_NOT_FOUND));
        Study study = studyRepository.findById(schedule.getStudy().getId()).orElseThrow(
                () -> new NoSuchElementException(ExceptionUtil.STUDY_NOT_FOUND)
        );

        if (scheduleRepository.existsByStudyAndStartedAtDate(
                study, LocalDate.of(
                        scheduleUpdateReq.getStartedAt().getYear(),
                        scheduleUpdateReq.getStartedAt().getMonth(),
                        scheduleUpdateReq.getStartedAt().getDayOfMonth())
        )) {
            throw new DuplicateFormatFlagsException(ExceptionUtil.STUDY_DATE_DUPLICATE);
        }

//      스터디장만 수정 가능
        schedule.setStartedAt(scheduleUpdateReq.getStartedAt());
        schedule.setFinishedAt(scheduleUpdateReq.getFinishedAt());
        scheduleRepository.save(schedule);
        for(String problemid:scheduleUpdateReq.getToSolveProblems())
        {
            ToSolveProblem toSolveProblem = toSolveProblemRepository.save(
                    ToSolveProblem.builder()
                            .isSolved(false)
                            .schedule(schedule)
                            .problemId(problemid)
                            .build());
        };
    }

    public ScheduleRes getSchedule(String id) throws IllegalAccessException {
        Schedule schedule = scheduleRepository.findById(Long.valueOf(id)).orElseThrow(() -> new NoSuchElementException(ExceptionUtil.SCHEDULE_NOT_FOUND));
        return ScheduleRes.builder()
                .startedAt(schedule.getStartedAt())
                .finishedAt(schedule.getFinishedAt())
                .build();
    }

    public List<ScheduleListRes> getScheduleMonthList(ScheduleListReq scheduleListReq) {
        Study study = studyRepository.findById(scheduleListReq.getStudyId()).orElseThrow(
                () -> new NoSuchElementException(ExceptionUtil.STUDY_NOT_FOUND)
        );

        return ScheduleListRes.of(scheduleRepository.findAllByStudyAndStartedAt_YearAndStartedAt_MonthOrderByStartedAtAsc(
                study, scheduleListReq.getYear(), Month.of(scheduleListReq.getMonth())
        ));
    }

    public void deleteSchedule(Long id) {
        List<Code> codes = codeRepository.findByUserId(id);
        Schedule schedule = scheduleRepository.findById(id).orElseThrow(() -> new NoSuchElementException(ExceptionUtil.SCHEDULE_NOT_FOUND));
        scheduleRepository.delete(schedule);
        codeRepository.deleteAll(codes);
    }
}
