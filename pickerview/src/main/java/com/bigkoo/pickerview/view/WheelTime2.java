package com.bigkoo.pickerview.view;

import android.util.Log;
import android.view.View;

import com.bigkoo.pickerview.R;
import com.bigkoo.pickerview.adapter.CalendarWheelAdapter;
import com.bigkoo.pickerview.listener.ISelectTimeCallback;
import com.bigkoo.pickerview.lunar.LunarCalendar;
import com.contrarywind.view.WheelView;

import java.util.Calendar;
import java.util.Date;

public class WheelTime2 {

    public static int MODE_DAY_IN_WEEK = 1;
    public static int MODE_DAY_IN_MONTH = 2;
    public static int MODE_WEEK = 4;
    public static int MODE_MONTH = 8;
    public static int MODE_YEAR = 16;
    public static int MODE_COMBINE = 32;

    public static int MODE_SECOND = 1;
    public static int MODE_MINUTE = 2;
    public static int MODE_HOUR = 4;
    public static int MODE_AMPM = 8;

    private static final int[] viewIds = {
        R.id.second,
        R.id.min,
        R.id.hour,
        R.id.ampm,
        R.id.day,
        R.id.week,
        R.id.month,
        R.id.year,
    };

    private static final String TAG = "WheelTime2";
    private static final String[] weekNum = {"日", "一", "二", "三", "四", "五", "六"};

    private final View view;
    private final WheelView[] wheelViews = new WheelView[8];
    private String[] labels = {
        "秒", "分", "时", "上午/下午",
        "x日 周x", "第x周", "月", "年"
    };

    private int dateMode;
    private int timeMode;
    private int timeInterval;

    private final CalendarWheelAdapter.UnionState adpaterState;

    private boolean isLunarCalendar = false;
    private ISelectTimeCallback mSelectChangeCallback;

    public WheelTime2(View view, boolean[] type, int gravity, int textSize) {
        super();
        this.view = view;

        for (int i = 0; i < viewIds.length; ++i) {
            wheelViews[i] = view.findViewById(viewIds[i]);
            wheelViews[i].setTextSize(textSize);
            wheelViews[i].setGravity(gravity);
        }

        adpaterState = new CalendarWheelAdapter.UnionState();
        int mode = MODE_YEAR;
        for (int i = 0; i < 6; ++i) {
            if (type[i]) {
                if (i < 3)
                    dateMode |= mode;
                else
                    timeMode |= mode;
            }
            mode >>= 1;
            if (i == 1)
                mode >>= 1;
            else if (i == 2)
                mode = MODE_HOUR;
        }
        syncMode();
    }

    public void setMode(int date, int time) {
        adpaterState.detachAll();
        dateMode = date;
        timeMode = time;
        syncMode();
    }

    public void setInterval(int interval) {
        timeInterval = interval;
        if ((timeMode & (MODE_HOUR | MODE_MINUTE | MODE_SECOND)) != 0) {
            adpaterState.lastItem().setInterval(interval);
        }
    }

    public void setLunarMode(boolean isLunarCalendar) {
        if (this.isLunarCalendar == isLunarCalendar)
            return;
        this.isLunarCalendar = isLunarCalendar;
        adpaterState.setLunar(isLunarCalendar);
        syncMode();
    }

    public boolean isLunarMode() {
        return isLunarCalendar;
    }

    public void setPicker(int year, final int month, int day, int h, int m, int s) {
        //setSolar(year, month, day, h, m, s);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, h, m, s);
        adpaterState.setCurrent(cal);
    }

    private static final CalendarWheelAdapter.Field[][] allFields = {
        new CalendarWheelAdapter.Field[] {
            CalendarWheelAdapter.Field.DAY_OF_WEEK,
            CalendarWheelAdapter.Field.DAY_OF_MONTH,
            CalendarWheelAdapter.Field.DAY_OF_YEAR,
            CalendarWheelAdapter.Field.DAY,
        },
        new CalendarWheelAdapter.Field[] {
            CalendarWheelAdapter.Field.WEEK_OF_MONTH,
            CalendarWheelAdapter.Field.WEEK_OF_YEAR,
            CalendarWheelAdapter.Field.WEEK,
        },
        new CalendarWheelAdapter.Field[] {
            CalendarWheelAdapter.Field.MONTH_OF_YEAR,
            CalendarWheelAdapter.Field.MONTH,
        },
        new CalendarWheelAdapter.Field[] {
            CalendarWheelAdapter.Field.YEAR,
        },
    };

    private void syncMode() {

        CalendarWheelAdapter.Field[] fields = {
            CalendarWheelAdapter.Field.SECOND,
            CalendarWheelAdapter.Field.MINUTE,
            CalendarWheelAdapter.Field.HOUR_OF_DAY,
            CalendarWheelAdapter.Field.AM_PM,
            null, null, null, null,
        };
        CalendarWheelAdapter.Formatter[] formatters = new CalendarWheelAdapter.Formatter[8];

        // time is simple
        for (int ti = 0; ti < 4; ++ti) {
            if ((timeMode & (1 << ti)) == 0)
                fields[ti] = null;
        }
        if ((timeMode & (MODE_AMPM | MODE_HOUR)) == (MODE_AMPM | MODE_HOUR)) {
            fields[2] = CalendarWheelAdapter.Field.HOUR;
        }
        if (fields[3] != null) {
            final String[] t = labels[3].split("/");
            formatters[3] = calendar -> {
                int n = calendar.get(Calendar.AM_PM);
                return n == Calendar.AM ? t[0] : t[1];
            };
        }
        // date
        // ignore week for lunar
        int mode = isLunarCalendar ? MODE_DAY_IN_MONTH : (MODE_DAY_IN_WEEK | MODE_DAY_IN_MONTH);
        int day = dateMode & mode;
        int monwk = (dateMode & (MODE_WEEK | MODE_MONTH)) >> 2;
        int day2 = monwk == 0 ? day : (day & monwk);
        int dayFm = day | monwk;
        // first char is day mode ( of week or month )
        // second char is additional label
        // + is same as additional char, but day2 == 0
        // int combine mode ( diff only day == 0)
        //
        //    day   0   1   2   3
        // monwk
        //  0           W   M   WM
        //  1     [Y    W   W+  WM
        //  2     [Y    M+  M   MW
        //  3     [M    WM  W   WM
        int dateMode2 = ((dateMode >> 1) & 0b11110) | (day2 == 0 ? 0 : 1);
        if ((dateMode & MODE_COMBINE) == 0) {
            int lastField = -1;
            for (int i = 0; i < 4; ++i) {
                if ((dateMode2 & (1 << i)) != 0) {
                    lastField = i + 4;
                }
                if (lastField >= 0) {
                    fields[lastField] = allFields[lastField - 4][i + 4 - lastField];
                    Log.d(TAG, "syncMode fields[" + (lastField) + "]=" + fields[lastField].toString());
                }
            }
        } else {
            Log.d(TAG, "syncMode combine " + dateMode2);
            int curFields = 0;
            int indexField = 0;
            int indexMode = -1;
            for (int i = 0; i < 5; ++i) {
                if ((dateMode2 & (1 << i)) == 0) {
                    ++indexField;
                } else {
                    if (indexMode >= 0) {
                        fields[indexMode + 4] = allFields[curFields][indexField];
                        Log.d(TAG, "syncMode fields[" + (indexMode + 4) + "]=" + fields[indexMode + 4].toString());
                        curFields += indexField + 1;
                        indexField = 0;
                    }
                    indexMode = i;
                }
            }
        }
        // day
        formatters[4] = dayFormatter(dayFm, labels[4]);
        // week
        if (fields[6] == null) {
            formatters[5] = calendar -> labels[5].replace("x",
                String.valueOf(calendar.get(Calendar.WEEK_OF_YEAR)));
        } else {
            formatters[5] = calendar -> labels[5].replace("x",
                String.valueOf(calendar.get(Calendar.WEEK_OF_MONTH)));
        }
        // month
        if (isLunarCalendar) {
            formatters[6] = calendar ->((LunarCalendar) calendar).getMonthName();
        } else {
            formatters[6] = calendar -> (calendar.get(Calendar.MONTH) + 1) + labels[6];
        }
        // year
        if ((dateMode & MODE_YEAR) != 0) {
            fields[7] = CalendarWheelAdapter.Field.YEAR;
            if (isLunarCalendar) {
                formatters[7] = calendar -> ((LunarCalendar) calendar).getYearName();
            } else {
                formatters[7] = calendar -> calendar.get(Calendar.YEAR) + labels[7];
            }
        }
        // handle combine
        if ((dateMode & MODE_COMBINE) != 0) {
            if (day2 == 0) {
                // merge day to parent
                if (fields[5] != null) {
                    formatters[5] = joinFormatter(formatters[5], formatters[4]);
                } else if (fields[6] != null) {
                    formatters[6] = joinFormatter(formatters[6], formatters[4]);
                }
            }
            if (monwk == 0) {
                // merge month or week to year
                int i = 4; // assert(fields[4] != null)
                if (fields[6] != null) {
                    i = 6;
                } else if (fields[5] != null) {
                    i = 5;
                }
                if (fields[7] != null) {
                    formatters[7] = joinFormatter(formatters[7], formatters[i]);
                }
            }
        }
        // bindAdapter
        int last = 8;
        for (int i = 7; i >=0; --i) {
            wheelViews[i].setLabel(labels[i]);
            bindAdapter(wheelViews[i], fields[i], formatters[i]);
            if (fields[i] != null)
                last = i;
        }
        if (last < 3)
            ((CalendarWheelAdapter) wheelViews[last].getAdapter()).setInterval(timeInterval);
    }

    private CalendarWheelAdapter.Formatter joinFormatter(CalendarWheelAdapter.Formatter formatter,
                                                         CalendarWheelAdapter.Formatter formatter1) {
        return calendar -> formatter.format(calendar) + " " + formatter1.format(calendar);
    }

    private CalendarWheelAdapter.Formatter dayFormatter(int dayFm, String label) {
        if (isLunarCalendar) {
            return calendar -> ((LunarCalendar) calendar).getDayName();
        }
        final String[] t = label.split(" ");
        return calendar -> {
            String r = "";
            if ((dayFm & 2) != 0) {
                int dm = calendar.get(Calendar.DAY_OF_MONTH);
                r += t[0].replace("x", String.valueOf(dm));
            }
            if (dayFm == 3)
                r += " ";
            if ((dayFm & 1) != 0) {
                int dw = calendar.get(Calendar.DAY_OF_WEEK);
                r += t[1].replace("x", weekNum[dw - 1]);
            }
            return r;
        };
    }

    private void bindAdapter(WheelView view, CalendarWheelAdapter.Field field, CalendarWheelAdapter.Formatter formatter) {
        if (field == null) {
            view.setAdapter(null);
            view.setVisibility(View.GONE);
            return;
        }
        CalendarWheelAdapter adapter = new CalendarWheelAdapter(adpaterState, field, formatter, (adapter2) -> {
            view.setAdapter(adapter2);
            view.setCurrentItem(adapter2.getCurrent());
        });
        if (formatter != null)
            view.setLabel("");
        view.setVisibility(View.VISIBLE);
        view.setOnItemSelectedListener((index) -> {
            adapter.setCurrent(index);
            mSelectChangeCallback.onTimeSelectChanged();
        });
    }

    public void setLabels(String[] labels) {
        if (labels == null || labels.length != 8)
            return;
        this.labels = labels;
        if (isLunarCalendar) {
            return;
        }
        syncMode();
    }

    public void setTextXOffset(int[] offsets) {
        if (offsets == null || offsets.length != 8)
            return;
        for (int i = 0; i < 8; ++i) {
            wheelViews[i].setTextXOffset(offsets[i]);
        }
    }

    /**
     * 设置是否循环滚动
     *
     * @param cyclic cyclic
     */
    public void setCyclic(boolean cyclic) {
        for (WheelView wv : wheelViews)
            wv.setCyclic(cyclic);
    }

    public Date getTime() {
        if (adpaterState.current() == null)
            return null;
        return adpaterState.current().getTime();
    }


    public View getView() {
        return view;
    }

    public void setRangDate(Calendar startDate, Calendar endDate) {
        adpaterState.setRange(startDate, endDate);
    }

    /**
     * 设置间距倍数,但是只能在1.0-4.0f之间
     *
     * @param lineSpacingMultiplier lineSpacingMultiplier
     */
    public void setLineSpacingMultiplier(float lineSpacingMultiplier) {
        for (WheelView wv : wheelViews)
            wv.setLineSpacingMultiplier(lineSpacingMultiplier);
    }

    /**
     * 设置分割线的颜色
     *
     * @param dividerColor dividerColor
     */
    public void setDividerColor(int dividerColor) {
        for (WheelView wv : wheelViews)
            wv.setDividerColor(dividerColor);
    }

    /**
     * 设置分割线的类型
     *
     * @param dividerType dividerType
     */
    public void setDividerType(WheelView.DividerType dividerType) {
        for (WheelView wv : wheelViews)
            wv.setDividerType(dividerType);
    }

    /**
     * 设置分割线之间的文字的颜色
     *
     * @param textColorCenter textColorCenter
     */
    public void setTextColorCenter(int textColorCenter) {
        for (WheelView wv : wheelViews)
            wv.setTextColorCenter(textColorCenter);
    }

    /**
     * 设置分割线以外文字的颜色
     *
     * @param textColorOut textColorCenter
     */
    public void setTextColorOut(int textColorOut) {
        for (WheelView wv : wheelViews)
            wv.setTextColorOut(textColorOut);
    }

    /**
     * @param isCenterLabel 是否只显示中间选中项的
     */
    public void isCenterLabel(boolean isCenterLabel) {
        for (WheelView wv : wheelViews)
            wv.isCenterLabel(isCenterLabel);
    }

    public void setSelectChangeCallback(ISelectTimeCallback mSelectChangeCallback) {
        this.mSelectChangeCallback = mSelectChangeCallback;
    }

    public void setItemsVisible(int itemsVisibleCount) {
        for (WheelView wv : wheelViews)
            wv.setItemsVisibleCount(itemsVisibleCount);
    }

    public void setAlphaGradient(boolean isAlphaGradient) {
        for (WheelView wv : wheelViews)
            wv.setAlphaGradient(isAlphaGradient);
    }
}
