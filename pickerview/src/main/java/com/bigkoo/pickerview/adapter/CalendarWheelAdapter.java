package com.bigkoo.pickerview.adapter;

import com.bigkoo.pickerview.lunar.LunarCalendar;
import com.contrarywind.adapter.WheelAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarWheelAdapter implements WheelAdapter<Object> {

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
                if (c.before(start))
                    c = (Calendar) start.clone();
                else if (current.after(end))
                    c = (Calendar) end.clone();
                current = null;
                setCurrent(c);
            }
        }

        public void setCurrent(Calendar day) {
            if (start == null || end == null)
                return;
            day = checkLunarSolar(day);
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
            if (field >= Calendar.FIELD_COUNT) {
                getAllMinMax(field, values);
                return;
            }
            if (field == Calendar.YEAR) {
                values[0] = start.get(field);
                values[1] = end.get(field);
                return;
            }
            int cur = current.get(field);
            int min = current.getActualMinimum(field);
            int max = current.getActualMaximum(field);
            current.add(field, min - cur);
            if (current.before(start)) {
                min = start.get(field);
            }
            current.add(field, max - min);
            if (current.after(end)) {
                max = end.get(field);
            }
            current.add(field, cur - max);
            values[0] = min;
            values[1] = max;
        }

        private void getAllMinMax(int field, int[] values) {
            values[0] = 0;
            if (field == Calendar.FIELD_COUNT) { // Month
                int s = start.get(Calendar.MONTH) + start.get(Calendar.YEAR) * 12;
                int e = end.get(Calendar.MONTH) + end.get(Calendar.YEAR) * 12;
                values[1] = e - s;
            } else {
                long days = (end.getTime().getTime() - start.getTime().getTime());
                days = (days + MS_IN_DAY - 1) / MS_IN_DAY;
                if (field == Calendar.FIELD_COUNT + 1) { // Week
                    values[1] = (int) ((days + 6) / 7);
                } else {
                    values[1] = (int) days;
                }
            }
        }

        private void add(Calendar cal, int field, int i) {
            if (field >= Calendar.FIELD_COUNT) {
                addAll(cal, field, i);
            } else {
                cal.add(field, i);
            }
        }

        private void addAll(Calendar cal, int field, int i) {
            if (field == Calendar.FIELD_COUNT) {
                cal.add(Calendar.MONTH, i);
            } else {
                if (field == Calendar.FIELD_COUNT + 1)
                    i *= 7;
                cal.add(Calendar.DAY_OF_YEAR, i);
            }
        }

        int get(int field) {
            return get(current, field);
        }

        private int get(Calendar cal, int field) {
            if (field >= Calendar.FIELD_COUNT) {
                return getAll(cal, field);
            } else {
                return cal.get(field);
            }
        }

        private int getAll(Calendar cal, int field) {
            if (field == Calendar.FIELD_COUNT) {
                int s = start.get(Calendar.MONTH) + start.get(Calendar.YEAR) * 12;
                int e = cal.get(Calendar.MONTH) + cal.get(Calendar.YEAR) * 12;
                return e - s;
            } else {
                long days = (cal.getTime().getTime() - start.getTime().getTime());
                days = (days + MS_IN_DAY - 1) / MS_IN_DAY;
                if (field == Calendar.FIELD_COUNT + 1)
                    return (int) ((days + 6) / 7);
                return (int) days;
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

    public int minValue() {
        return minMax[0];
    }

    public int maxValue() {
        return minMax[1];
    }

    public int curValue() {
        return calendar.get(field.value);
    }

    public void setCurrent(int index) {
        calendar.setCurrent(field.value, minMax[0] + index);
    }

    public void setInterval(int interval) {
        this.interval = interval;
        listener.dataSetChanged(this);
    }

    private void notifyDataSetChanged() {
        calendar.getMinMax(field.value, minMax);
        last = (Calendar) calendar.current.clone();
        listener.dataSetChanged(this);
    }

    @Override
    public int getItemsCount() {
        return (minMax[1] - minMax[0] + interval) / interval;
    }

    @Override
    public Object getItem(int index) {
        int l = calendar.get(last, field.value);
        calendar.add(last, field.value, minMax[0] + index * interval - l);
        if (formatter != null)
            return formatter.format(last);
        return calendar.getNormal(last, field.value);
    }

    @Override
    public int indexOf(Object o) {
        return 0;
    }

}
