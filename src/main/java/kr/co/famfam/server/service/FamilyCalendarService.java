package kr.co.famfam.server.service;

import kr.co.famfam.server.domain.FamilyCalendar;
import kr.co.famfam.server.model.CalendarReq;
import kr.co.famfam.server.model.DefaultRes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by ehay@naver.com on 2018-12-25
 * Blog : http://ehay.tistory.com
 * Github : http://github.com/ehayand
 */

public interface FamilyCalendarService {

    List<FamilyCalendar> findByYearAndMonth(final LocalDateTime startDate, final LocalDateTime endDate, final int groupIdx);

    List<FamilyCalendar> findByYearAndMonthAndDate(final String dateStr, final int groupIdx);

    DefaultRes addSchedule(final CalendarReq calendarReq, final int authUserIdx, final String allDateStr);

    DefaultRes updateSchedule(final int calendarIdx, final CalendarReq calendarReq, final String allDateStr);

    DefaultRes deleteSchedule(final int calendarIdx);

    List<FamilyCalendar> searchSchedule(final String content, final int groupIdx);
}
