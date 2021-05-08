package com.bigkoo.pickerview.adapter;

import com.bigkoo.pickerview.lunar.LunarCalendar;
import com.contrarywind.adapter.WheelAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarWheelAdapter implements WheelAdapter<Object> {

    public static final int MAX_YEAR = 2200;
    public static final int MIN_YEAR = 1900;

    public enum Field {
        YEAR(Calendar.YEAR),
        MONTH(Calendar.FIELD_COUNT), // ALL MONTH
        MONTH_OF_YEAR(Calendar.MONTH),
        WEEK(Calendar.FIELD_COUNT + 1), // ALL WEEK
        WEEK_OF_YEAR(Calendar.WEEK_OF_YEAR),
        WEEK_OF_MONTH(Calendar.WEEK_OF_MONTH),
        DAY(Calendar.FIELD_COUNT + 2), // ALL DAY
        DAY_OF_MONTH(Calendar.DAY_OF_MONTH),
        DAY_OF_WEEK(Calendar.DAY_OF_WEEK),
        DAY_OF_YEAR(Calendar.DAY_OF_YEAR),
        AM_PM(Calendar.AM_PM),
        HOUR(Calendar.HOUR), // 12 hours
        HOUR_OF_DAY(Calendar.HOUR_OF_DAY),  // 24 hours
        MINUTE(Calendar.MINUTE),
        SECOND(Calendar.SECOND);

        Field(int value) {
            this.value = value;
        }

        public int value;
    }

    public static class UnionState {

        boolean isLunar;
        Calendar start;
        Calendar current;
        Calendar end;

        public void setLunar(boolean lunar) {
            detachAll();
            if (isLunar == lunar)
                return;
            isLunar = lunar;
            if (lunar) {
                start = toLunar(start);
                end = toLunar(end);
                current = toLunar(current);
            } else {
                start = toSolar(start);
                end = toSolar(end);
                current = toSolar(current);
            }
        }

        public void setRange(Calendar startDate, Calendar endDate) {
            start = checkLunarSolar(startDate);
            end = checkLunarSolar(endDate);
            if (current != null) {
                Calendar c = current;
                if (start != null && c.before(start))
                    c = (Calendar) start.clone();
                else if (end != null && current.after(end))
                    c = (Calendar) end.clone();
                current = null;
                setCurrent(c);
            }
        }

        public void setCurrent(Calendar day) {
            day = checkLunarSolar(day);
            if (day.before(start))
                day = (Calendar) start.clone();
            else if (day.after(end))
                day = (Calendar) end.clone();
            boolean diff = false;
            if (current == null) {
                current = (Calendar) day.clone();
                diff = true;
            }
            for (CalendarWheelAdapter adapter : adapterList) {
                if (diff) {
                    adapter.notifyDataSetChanged();
                    continue;
                }
                int f = adapter.field.value;
                if (get(day, f) == get(current, f)) {
                    continue;
                }
                current = day;
                diff = true;
                adapter.notifyDataSetChanged();
            }
        }

        public void detachAll() {
            adapterList.clear();
        }

        public CalendarWheelAdapter lastItem() {
            return adapterList.get(adapterList.size() - 1);
        }

        public Calendar current() {
            return current;
        }

        private static final int MS_IN_DAY = 24 * 3600 * 1000;

        void getMinMax(int field, int[] values) {
            switch (field) {
                case Calendar.FIELD_COUNT: {
                    values[0] = 0;
                    int s = start == null ? MIN_YEAR * 12 : (start.get(Calendar.MONTH) + start.get(Calendar.YEAR) * 12);
                    int e = end == null ? MAX_YEAR * 12 : (end.get(Calendar.MONTH) + end.get(Calendar.YEAR) * 12);
                    values[1] = e - s;
                    break;
                }
                case Calendar.FIELD_COUNT + 1:
                case Calendar.FIELD_COUNT + 2: {
                    // TODO:
                    values[0] = 0;
                    long days = (end == null ? (MAX_YEAR - MIN_YEAR) * 365
                        : end.getTimeInMillis() - (start == null ? 0 : start.getTimeInMillis() / MS_IN_DAY * MS_IN_DAY));
                    days = days / MS_IN_DAY;
                    if (field == Calendar.FIELD_COUNT + 1) { // Week
                        values[1] = (int) (days / 7);
                    } else {
                        values[1] = (int) days;
                    }
                    break;
                }
                case Calendar.YEAR:
                    values[0] = start == null ? 1900 : start.get(field);
                    values[1] = end == null ? 2200 : end.get(field);
                    break;
                default: {
                    int cur = current.get(field);
                    int ori = cur;
                    int min = current.getActualMinimum(field);
                    int max = current.getActualMaximum(field);
                    current.add(field, min - cur);
                    cur = current.get(field);
                    if (start != null && current.before(start)) {
                        min = start.get(field);
                    }
                    current.add(field, max - cur);
                    cur = current.get(field);
                    if (end != null && current.after(end)) {
                        max = end.get(field);
                    }
                    current.add(field, ori - cur);
                    cur = current.get(field);
                    values[0] = min;
                    values[1] = max;
                    break;
                }
            }
        }

        private void add(Calendar cal, int field, int i) {
            switch (field) {
                case Calendar.FIELD_COUNT:
                    cal.add(Calendar.MONTH, i);
                    break;
                case Calendar.FIELD_COUNT + 1:
                    i *= 7;
                case Calendar.FIELD_COUNT + 2:
                    cal.add(Calendar.DAY_OF_YEAR, i);
                    break;
                default:
                    cal.add(field, i);
            }
        }

        int get(int field) {
            return get(current, field);
        }

        private int get(Calendar cal, int field) {
            switch (field) {
                case Calendar.FIELD_COUNT:
                    int s = start == null ? MIN_YEAR * 12 : (start.get(Calendar.MONTH) + start.get(Calendar.YEAR) * 12);
                    int e = cal.get(Calendar.MONTH) + cal.get(Calendar.YEAR) * 12;
                    return e - s;
                case Calendar.FIELD_COUNT + 1:
                case Calendar.FIELD_COUNT + 2:
                    long days = (cal.getTimeInMillis() - (start == null ? 0 : start.getTimeInMillis() / MS_IN_DAY * MS_IN_DAY));
                    days = days / MS_IN_DAY;
                    if (field == Calendar.FIELD_COUNT + 1)
                        return (int) (days / 7);
                    return (int) days;
                default:
                    return cal.get(field);
            }
        }

        private static Calendar toSolar(Calendar cal) {
            if (cal == null)
                return null;
            if (cal instanceof LunarCalendar) {
                Calendar c = Calendar.getInstance();
                c.setTime(cal.getTime());
                return c;
            }
            return cal;
        }

        private static Calendar toLunar(Calendar cal) {
            if (cal == null)
                return null;
            if (cal instanceof LunarCalendar)
                return cal;
            return new LunarCalendar(cal);
        }

        private Calendar checkLunarSolar(Calendar cal) {
            if (isLunar)
                return toLunar(cal);
            return toSolar(cal);
        }

        private final List<CalendarWheelAdapter> adapterList = new ArrayList<>();

        void attach(CalendarWheelAdapter adapter) {
            adapterList.add(adapter);
            if (current != null)
                adapter.notifyDataSetChanged();
        }

        void setCurrent(int field, int value) {
            int cur = get(current, field);
            Calendar cal = (Calendar) current.clone();
            add(cal, field, value - cur);
            setCurrent(cal);
        }

        int getNormal(Calendar cal, int field) {
            if (field == Calendar.FIELD_COUNT)
                field = Calendar.MONTH;
            else if (field == Calendar.FIELD_COUNT + 1)
                field = Calendar.WEEK_OF_YEAR;
            else if (field == Calendar.FIELD_COUNT + 2)
                field = Calendar.DAY_OF_YEAR;
            return cal.get(field);
        }
    }

    @FunctionalInterface
    public interface Formatter {
        String format(Calendar calendar);
    }

    @FunctionalInterface
    public interface DataSetChangeListener {
        void dataSetChanged(CalendarWheelAdapter adapter);
    }

    UnionState calendar;
    Field field;
    Formatter formatter;
    DataSetChangeListener listener;

    int[] minMax = new int[2];
    int interval = 1;
    Calendar last;

    public CalendarWheelAdapter(UnionState calendar, Field field, Formatter formatter, DataSetChangeListener listener) {
        this.calendar = calendar;
        this.field = field;
        this.formatter = formatter;
        this.listener = listener;
        minMax[0] = 0;
        minMax[1] = -1;
        this.calendar.attach(this);
    }

    public void setInterval(int interval) {
        this.interval = interval;
        if (calendar.current != null)
            listener.dataSetChanged(this);
    }

    @Override
    public int getItemsCount() {
        int min = (minMax[0] + interval - 1) / interval;
        int max = minMax[1] / interval;
        return max - min + 1;
    }

    public int getAt(int index) {
        int min = (minMax[0] + interval - 1) / interval;
        return (min + index) * interval;
    }

    public int indexOf(int value) {
        int min = (minMax[0] + interval - 1) / interval;
        // will return -1 if current < next interval
        return value / interval - min;
    }

    public int getCurrent() {
        return indexOf(calendar.get(field.value));
    }

    public void setCurrent(int index) {
        calendar.setCurrent(field.value, getAt(index));
    }

    private void notifyDataSetChanged() {
        calendar.getMinMax(field.value, minMax);
        last = (Calendar) calendar.current.clone();
        listener.dataSetChanged(this);
    }

    @Override
    public Object getItem(int index) {
        int l = calendar.get(last, field.value);
        calendar.add(last, field.value, getAt(index) - l);
        if (formatter != null)
            return formatter.format(last);
        return calendar.getNormal(last, field.value);
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

}
