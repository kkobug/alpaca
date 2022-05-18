package com.ssafy.alpaca.api.service;

import com.ssafy.alpaca.api.request.ScheduleUpdateReq;
import com.ssafy.alpaca.api.request.ScheduleReq;
import com.ssafy.alpaca.api.response.ProblemListRes;
import com.ssafy.alpaca.api.response.ScheduleRes;
import com.ssafy.alpaca.api.response.ScheduleListRes;
import com.ssafy.alpaca.api.response.UserListRes;
import com.ssafy.alpaca.common.exception.UnAuthorizedException;
import com.ssafy.alpaca.common.util.ConvertUtil;
import com.ssafy.alpaca.common.util.ExceptionUtil;
import com.ssafy.alpaca.db.document.Problem;
import com.ssafy.alpaca.db.entity.*;
import com.ssafy.alpaca.db.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final UserRepository userRepository;
    private final MyStudyRepository myStudyRepository;
    private final StudyRepository studyRepository;
    private final ScheduleRepository scheduleRepository;
    private final ToSolveProblemRepository toSolveProblemRepository;
    private final SolvedProblemRepository solvedProblemRepository;
    private final ProblemRepository problemRepository;
    private final CodeRepository codeRepository;
    private final ConvertUtil convertUtil;
    private final NotificationService notificationService;

    private User checkUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                () -> new NoSuchElementException(ExceptionUtil.USER_NOT_FOUND)
        );
    }

    private Schedule checkScheduleById(Long id) {
        return scheduleRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(ExceptionUtil.SCHEDULE_NOT_FOUND)
        );
    }

    private Study checkStudyById(Long id) {
        return studyRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException(ExceptionUtil.STUDY_NOT_FOUND)
        );
    }

    private void checkIsStudyMember(User user, Study study)  {
        if (Boolean.TRUE.equals(!myStudyRepository.existsByUserAndStudy(user, study))) {
            throw new UnAuthorizedException(ExceptionUtil.UNAUTHORIZED_USER);
        }
    }

    private List<ProblemListRes> getProblemListByToSolveProblems(User user, List<ToSolveProblem> toSolveProblems, List<MyStudy> myStudies) {
        List<ProblemListRes> problemListResList = new ArrayList<>();
        HashSet<Long> solvedProblems = solvedProblemRepository.findProblemNumbersByUserId(user.getId());
        for (ToSolveProblem toSolveProblem : toSolveProblems) {
            Problem problem = problemRepository.findByProblemNumber(toSolveProblem.getProblemNumber()).orElseThrow(
                    () -> new NoSuchElementException(ExceptionUtil.PROBLEM_NOT_FOUND)
            );

            List<UserListRes> userListRes = new ArrayList<>();
            for (MyStudy myStudy : myStudies) {
                if (codeRepository.existsByProblemNumberAndUserId(problem.getProblemNumber(), myStudy.getUser().getId())) {
                    userListRes.add(
                            UserListRes.builder()
                                    .id(myStudy.getUser().getId())
                                    .nickname(myStudy.getUser().getNickname())
                                    .profileImg(convertUtil.convertByteArrayToString(myStudy.getUser().getProfileImg()))
                                    .build()
                    );
                }
            }

            problemListResList.add(
                    ProblemListRes.builder()
                            .problemNumber(problem.getProblemNumber())
                            .title(problem.getTitle())
                            .level(problem.getLevel())
                            .isSolved(solvedProblems.contains(problem.getProblemNumber()))
                            .solvedMemberList(userListRes)
                            .build()
            );
        }
        return problemListResList;
    }

    public Long createSchedule(String username, ScheduleReq scheduleReq) {
        LocalDateTime finishedAt = LocalDateTime.of(
                scheduleReq.getFinishedAt().getYear(),
                scheduleReq.getFinishedAt().getMonth(),
                scheduleReq.getFinishedAt().getDayOfMonth(),
                scheduleReq.getFinishedAt().getHour(),
                scheduleReq.getFinishedAt().getMinute()
        );
        LocalDateTime startedAt = LocalDateTime.of(
                scheduleReq.getStartedAt().getYear(),
                scheduleReq.getStartedAt().getMonth(),
                scheduleReq.getStartedAt().getDayOfMonth(),
                scheduleReq.getStartedAt().getHour(),
                scheduleReq.getStartedAt().getMinute()
        );
        if (finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(ExceptionUtil.INVALID_DATE_VALUE);
        }

        Study study = checkStudyById(scheduleReq.getStudyId());
        User user = checkUserByUsername(username);
        checkIsStudyMember(user, study);

        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDateTime.of(
                scheduleReq.getStartedAt().getYear(),
                scheduleReq.getStartedAt().getMonth(),
                scheduleReq.getStartedAt().getDayOfMonth(), 0, 0), ZoneOffset.of("Z"));
        if (Boolean.TRUE.equals(scheduleRepository.existsByStudyAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                study, offsetDateTime, offsetDateTime.plusDays(1)))
        ) {
            throw new NullPointerException(ExceptionUtil.STUDY_DATE_DUPLICATE);
        }

        Schedule schedule = Schedule.builder()
                        .study(study)
                        .startedAt(OffsetDateTime.of(startedAt, ZoneOffset.of("Z")))
                        .finishedAt(OffsetDateTime.of(finishedAt, ZoneOffset.of("Z")))
                        .build();
        List<ToSolveProblem> toSolveProblems = new ArrayList<>();
        for (Long number : scheduleReq.getToSolveProblems()) {
            toSolveProblems.add(
                    ToSolveProblem.builder()
                            .schedule(schedule)
                            .problemNumber(number)
                            .build()
            );
        }

        scheduleRepository.save(schedule);
        toSolveProblemRepository.saveAll(toSolveProblems);
        return schedule.getId();
    }

    public ScheduleRes getTodaySchedule(String username, Long studyId) {
        Study study = checkStudyById(studyId);
        User user = checkUserByUsername(username);
        checkIsStudyMember(user, study);

        LocalDateTime localDateTime = LocalDateTime.now();
        OffsetDateTime today = OffsetDateTime.of(LocalDateTime.of(
                localDateTime.getYear(),
                localDateTime.getMonth(),
                localDateTime.getDayOfMonth(), 0, 0), ZoneOffset.of("Z"));
        Optional<Schedule> todaySchedule = scheduleRepository.findByStudyAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                study, today, today.plusDays(1));

        if (todaySchedule.isEmpty()) {
            throw new NoSuchElementException(ExceptionUtil.SCHEDULE_NOT_FOUND);
        }

        List<MyStudy> myStudies = myStudyRepository.findAllByStudy(study);
        List<ToSolveProblem> toSolveProblem = toSolveProblemRepository.findAllBySchedule(todaySchedule.get());
        List<ProblemListRes> problemListRes = getProblemListByToSolveProblems(user, toSolveProblem, myStudies);

        return ScheduleRes.builder()
                .startedAt(todaySchedule.get().getStartedAt())
                .finishedAt(todaySchedule.get().getFinishedAt())
                .problemListRes(problemListRes)
                .build();
    }

    public void updateSchedule(String username, Long id, ScheduleUpdateReq scheduleUpdateReq) {
        OffsetDateTime finishedAt = OffsetDateTime.of(LocalDateTime.of(
                scheduleUpdateReq.getFinishedAt().getYear(),
                scheduleUpdateReq.getFinishedAt().getMonth(),
                scheduleUpdateReq.getFinishedAt().getDayOfMonth(),
                scheduleUpdateReq.getFinishedAt().getHour(),
                scheduleUpdateReq.getFinishedAt().getMinute()
        ), ZoneOffset.of("Z"));
        OffsetDateTime startedAt = OffsetDateTime.of(LocalDateTime.of(
                scheduleUpdateReq.getStartedAt().getYear(),
                scheduleUpdateReq.getStartedAt().getMonth(),
                scheduleUpdateReq.getStartedAt().getDayOfMonth(),
                scheduleUpdateReq.getStartedAt().getHour(),
                scheduleUpdateReq.getStartedAt().getMinute()
        ), ZoneOffset.of("Z"));
        if (finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(ExceptionUtil.INVALID_DATE_VALUE);
        }

        Schedule schedule = checkScheduleById(id);
        Study study = schedule.getStudy();

        OffsetDateTime localDateTime = OffsetDateTime.of(LocalDateTime.of(
                scheduleUpdateReq.getStartedAt().getYear(),
                scheduleUpdateReq.getStartedAt().getMonth(),
                scheduleUpdateReq.getStartedAt().getDayOfMonth(), 0, 0), ZoneOffset.of("Z"));
        Optional<Schedule> checkSchedule = scheduleRepository.findByStudyAndStartedAtGreaterThanEqualAndStartedAtLessThan(
                study, localDateTime, localDateTime.plusDays(1));

        if (checkSchedule.isEmpty() || !schedule.getId().equals(checkSchedule.get().getId())) {
            throw new NoSuchElementException(ExceptionUtil.SCHEDULE_NOT_FOUND);
        }

//      스터디원만 수정 가능
        User user = checkUserByUsername(username);
        checkIsStudyMember(user, study);

        schedule.setStartedAt(startedAt);
        schedule.setFinishedAt(finishedAt);
        scheduleRepository.save(schedule);
        HashSet<Long> originProblems = toSolveProblemRepository.findProblemNumbersByScheduleId(schedule.getId());
        HashSet<Long> newProblems = new HashSet<>(scheduleUpdateReq.getToSolveProblems());

        List<ToSolveProblem> saveList = new ArrayList<>();
        List<ToSolveProblem> deleteList = new ArrayList<>();

        for (Long originProblem : originProblems) {
            if (newProblems.contains(originProblem)) {
                continue;
            }
            toSolveProblemRepository.findByScheduleAndProblemNumber(schedule, originProblem).ifPresent(deleteList::add);
        }

        for (Long newProblem : newProblems) {
            if (originProblems.contains(newProblem)) {
                continue;
            }
            saveList.add(ToSolveProblem.builder()
                    .schedule(schedule)
                    .problemNumber(newProblem)
                    .build());
        }

        toSolveProblemRepository.saveAll(saveList);
        toSolveProblemRepository.deleteAll(deleteList);
    }

    public ScheduleRes getSchedule(String username, Long id) {
        User user = checkUserByUsername(username);
        Schedule schedule = checkScheduleById(id);
        List<MyStudy> myStudies = myStudyRepository.findAllByStudy(schedule.getStudy());
        List<ToSolveProblem> toSolveProblem = toSolveProblemRepository.findAllBySchedule(schedule);
        List<ProblemListRes> problemListRes = getProblemListByToSolveProblems(user, toSolveProblem, myStudies);

        return ScheduleRes.builder()
                .startedAt(schedule.getStartedAt())
                .finishedAt(schedule.getFinishedAt())
                .problemListRes(problemListRes)
                .build();
    }

    public List<ScheduleListRes> getScheduleList(String username, Long id, Integer year, Integer month, Integer day) {
        Study study = checkStudyById(id);
        User user = checkUserByUsername(username);
        checkIsStudyMember(user, study);

        if (day == null) {
            OffsetDateTime offsetDateTime = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.of("Z"));
            if (offsetDateTime.getDayOfWeek().getValue() < 7) {
                offsetDateTime = offsetDateTime.minusDays(offsetDateTime.getDayOfWeek().getValue());
            }
            return ScheduleListRes.of(scheduleRepository.findAllByStudyAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                    study, offsetDateTime, offsetDateTime.plusWeeks(6)));
        } else {
            OffsetDateTime offsetDateTime = OffsetDateTime.of(year, month, day, 0, 0, 0, 0, ZoneOffset.of("Z"));
            if (offsetDateTime.getDayOfWeek().getValue() < 7) {
                offsetDateTime = offsetDateTime.minusDays(offsetDateTime.getDayOfWeek().getValue());
            }
            return ScheduleListRes.of(scheduleRepository.findAllByStudyAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtAsc(
                    study, offsetDateTime, offsetDateTime.plusDays(7)));
        }
    }

    public void deleteSchedule(String username, Long id) {
        User user = checkUserByUsername(username);
        Schedule schedule = checkScheduleById(id);
        Study study = schedule.getStudy();

        if (Boolean.TRUE.equals(!myStudyRepository.existsByUserAndStudyAndIsRoomMaker(user, study, true))) {
            throw new UnAuthorizedException(ExceptionUtil.UNAUTHORIZED_USER);
        }

        scheduleRepository.delete(schedule);
    }
}
