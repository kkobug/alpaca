import { ArrowBackIosNew, ArrowForwardIos } from '@mui/icons-material';
import { Grid, IconButton, Stack, styled, useTheme } from '@mui/material';
import React, { useState } from 'react';
import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useParams } from 'react-router-dom';
import { customAxios } from '../../../Lib/customAxios';
import {
  Schedule,
  setDailySchedule,
  setDateRange,
  setSchedules,
  setSelectedDayIdx,
} from '../../../Redux/roomReducer';
import RoomMainCalendarDay from './RoomMainCalendarDay';
import RoomMainCalendarWeek from './RoomMainCalendarWeek';
import { BrowserView, MobileView } from 'react-device-detect';

export interface DailySchedule {
  day: Date;
  schedule?: Schedule;
}

const CalendarTitle = styled('div')(({ theme }) => ({
  display: 'flex',
  justifyContent: 'space-around',
  paddingRight: theme.spacing(1),
}));

const CalendarBox = styled(Grid)(({ theme }) => ({
  background: theme.palette.component,
  marginLeft: 0,
  marginTop: 0,
  width: '100%',
  paddingRight: theme.spacing(1),
  paddingBottom: theme.spacing(1),
  borderRadius: '0 0 10px 10px',
}));

function RoomMainCalendar() {
  const { roomId } = useParams();
  const theme = useTheme();
  const dispatch = useDispatch();

  const dateRange = useSelector((state: any) => state.room.dateRange);
  const schedules = useSelector((state: any) => state.room.schedules);

  // 현재 보고 있는 달력의 달을 확인하기 위한 변수
  const [nowDay, setNowDay] = useState<Date>(new Date());

  // request최적화를 위해 설정
  const [prevDay, setPrevDay] = useState<Date>(new Date());

  // 현재 달력의 날짜계산
  const getStartDate = () => {
    const startDate = new Date();
    startDate.setMonth(nowDay.getMonth());
    startDate.setDate(1);
    startDate.setDate(-startDate.getDay() + 1);

    const tempDateRange = [];

    for (let i = 0; i < 42; i++) {
      const temp: DailySchedule = {
        day: new Date(startDate),
      };
      tempDateRange.push(temp);
      startDate.setDate(startDate.getDate() + 1);
    }
    dispatch(setDateRange(tempDateRange));
  };

  // 달이 변할때 스케줄을 가져오는 함수
  const getMonthlySchedule = async () => {
    getStartDate();
    try {
      const res = await customAxios({
        method: 'get',
        url: `/schedule/${roomId}/span`,
        params: { month: nowDay.getMonth() + 1, year: nowDay.getFullYear() },
      });
      // console.log('change month: ', res);
      const tempSchedules: Schedule[] = [];
      res.data.forEach((schedule: Schedule) => {
        const startedAt = new Date(schedule.startedAt);
        const finishedAt = new Date(schedule.finishedAt);

        tempSchedules.push({ id: schedule.id, startedAt, finishedAt });
      });

      dispatch(setSchedules([...tempSchedules]));
    } catch (e: any) {
      // console.log(e.response);
    }
  };

  useEffect(() => {
    if (
      nowDay.getMonth() !== prevDay.getMonth() ||
      nowDay.getFullYear() !== prevDay.getFullYear()
    ) {
      getMonthlySchedule();
    }

    setPrevDay(new Date(nowDay));
  }, [nowDay]);

  useEffect(() => {
    getStartDate();
    return () => {
      dispatch(setSchedules([]));
    };
  }, []);

  useEffect(() => {
    dispatch(setDailySchedule());
  }, [schedules, dispatch]);

  return (
    <>
      <BrowserView style={{ width: '100%' }}>
        <Stack>
          <CalendarTitle>
            <IconButton
              size="large"
              sx={{ color: theme.palette.txt }}
              onClick={() => setNowDay((prev) => new Date(prev.setMonth(prev.getMonth() - 1)))}>
              <ArrowBackIosNew />
            </IconButton>
            <div style={{ textAlign: 'center' }}>
              <span>{nowDay.getFullYear()}년</span>
              <h5>{nowDay.getMonth() + 1}월</h5>
            </div>
            <IconButton
              size="large"
              sx={{ color: theme.palette.txt }}
              onClick={() => setNowDay((prev) => new Date(prev.setMonth(prev.getMonth() + 1)))}>
              <ArrowForwardIos />
            </IconButton>
          </CalendarTitle>
          <RoomMainCalendarWeek />
          <CalendarBox
            container
            columns={7}
            spacing={1}
            sx={{ paddingLeft: 0, paddingTop: theme.spacing(1) }}>
            {dateRange.map((dailySchedule: DailySchedule, idx: number) => (
              <RoomMainCalendarDay
                dailySchedule={dailySchedule}
                key={idx}
                nowDay={nowDay}
                onClick={() => dispatch(setSelectedDayIdx(idx))}
              />
            ))}
          </CalendarBox>
        </Stack>
      </BrowserView>
      <MobileView>
        <Stack>
          <CalendarTitle>
            <IconButton
              size="large"
              onClick={() => setNowDay((prev) => new Date(prev.setMonth(prev.getMonth() - 1)))}>
              <ArrowBackIosNew />
            </IconButton>
            <div style={{ textAlign: 'center' }}>
              <span>{nowDay.getFullYear()}년</span>
              <h3>{nowDay.getMonth() + 1}월</h3>
            </div>
            <IconButton
              size="large"
              onClick={() => setNowDay((prev) => new Date(prev.setMonth(prev.getMonth() + 1)))}>
              <ArrowForwardIos />
            </IconButton>
          </CalendarTitle>
          <RoomMainCalendarWeek />
          <CalendarBox
            container
            columns={7}
            spacing={1}
            sx={{ paddingLeft: 0, paddingTop: theme.spacing(1) }}>
            {dateRange.map((dailySchedule: DailySchedule, idx: number) => (
              <RoomMainCalendarDay
                dailySchedule={dailySchedule}
                key={idx}
                nowDay={nowDay}
                onClick={() => dispatch(setSelectedDayIdx(idx))}
              />
            ))}
          </CalendarBox>
        </Stack>
      </MobileView>
    </>
  );
}

export default RoomMainCalendar;
