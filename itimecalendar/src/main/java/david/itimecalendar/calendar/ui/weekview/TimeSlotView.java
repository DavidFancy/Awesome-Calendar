package david.itimecalendar.calendar.ui.weekview;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.developer.paul.itimerecycleviewgroup.ITimeRecycleViewGroup;
import com.github.sundeepk.compactcalendarview.ITimeTimeslotCalendar.InnerCalendarTimeslotPackage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import david.itimecalendar.R;
import david.itimecalendar.calendar.listeners.ITimeCalendarTimeslotViewListener;
import david.itimecalendar.calendar.ui.monthview.DayViewBody;
import david.itimecalendar.calendar.ui.monthview.DayViewBodyCell;
import david.itimecalendar.calendar.listeners.ITimeTimeSlotInterface;
import david.itimecalendar.calendar.ui.unitviews.DraggableTimeSlotView;
import david.itimecalendar.calendar.ui.unitviews.TimeslotDurationEditView;
import david.itimecalendar.calendar.ui.unitviews.RcdRegularTimeSlotView;
import david.itimecalendar.calendar.ui.unitviews.TimeSlotInnerCalendarView;
import david.itimecalendar.calendar.ui.unitviews.TimeslotEditView;
import david.itimecalendar.calendar.util.DensityUtil;
import david.itimecalendar.calendar.util.MyCalendar;
import david.itimecalendar.calendar.wrapper.WrapperTimeSlot;

/**
 * Created by yuhaoliu on 30/05/2017.
 */

public class TimeSlotView extends WeekView {
    public static ViewMode mode = ViewMode.NON_ALL_DAY_CREATE;

    public enum ViewMode {
        ALL_DAY_CREATE, NON_ALL_DAY_CREATE, ALL_DAY_SELECT, NON_ALL_DAY_SELECT,
    }

    public boolean isTimeslotEnable = false;
    private ITimeCalendarTimeslotViewListener iTimeCalendarListener;

    private TimeSlotInnerCalendarView innerCalView;
    private TimeslotDurationEditView<String> durationBar;
    private View durationBarPlaceholder;
    private FrameLayout staticLayer;
    private InnerCalendarTimeslotPackage innerSlotPackage = new InnerCalendarTimeslotPackage();

    public TimeSlotView(@NonNull Context context) {
        super(context);
        setUpViews();
    }

    public TimeSlotView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setUpViews();
    }

    public TimeSlotView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUpViews();
    }

    private void setUpViews(){
        this.setLayoutTransition(new LayoutTransition());
        this.dayViewBody.setOnScrollListener(new ITimeRecycleViewGroup.OnScroll<DayViewBodyCell>() {
            @Override
            public void onPageSelected(DayViewBodyCell view) {
                innerCalView.setCurrentDate(view.getCalendar().getCalendar().getTime());
            }

            @Override
            public void onHorizontalScroll(int dx, int preOffsetX) {
                headerRG.followScrollByX(dx);
            }

            @Override
            public void onVerticalScroll(int dy, int preOffsetY) {

            }
        });
        setUpStaticLayer();
        setUpTimeslotDurationWidget();

//        enableTimeSlot(false);
    }

    private void setUpTimeslotDurationWidget(){
        int durationBarHeight = DensityUtil.dip2px(context,40);
        durationBar = new TimeslotDurationEditView<>(context);
        durationBar.setId(generateViewId());
        durationBar.setOptHeight(durationBarHeight);
        RelativeLayout.LayoutParams durationBarParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        durationBarParams.addRule(ALIGN_PARENT_BOTTOM);
        durationBar.setLayoutParams(durationBarParams);
        durationBar.setOnItemSelectedListener(new TimeslotDurationEditView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                DurationItem selectedItem = durationData.get(position);
                long toDuration = selectedItem.duration;

                if (onTimeslotDurationChangedListener != null){
                    onTimeslotDurationChangedListener.onTimeslotDurationChanged(toDuration);
                }
            }
        });
        this.addView(durationBar);

        //fake occupation view for header part of durationBar
        durationBarPlaceholder = new View(context);
        RelativeLayout.LayoutParams blankVieWParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, durationBarHeight);
        blankVieWParams.addRule(ALIGN_PARENT_BOTTOM);
        durationBarPlaceholder.setLayoutParams(blankVieWParams);
        durationBarPlaceholder.setId(View.generateViewId());
        this.addView(durationBarPlaceholder);
    }

    private void setUpStaticLayer(){
        //set up static layer
        staticLayer = new FrameLayout(context);
        RelativeLayout.LayoutParams stcPageParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        innerCalView = new TimeSlotInnerCalendarView(context);
        innerCalView.setOnTimeSlotInnerCalendar(new TimeSlotInnerCalendarView.OnTimeSlotInnerCalendar() {
            @Override
            public void onCalendarBtnClick(View v, boolean result) {
            }

            @Override
            public void onDayClick(Date dateClicked) {
                TimeSlotView.this.scrollToDate(dateClicked,true);
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
            }
        });
        innerCalView.setHeaderHeight((int)headerHeight);
        innerCalView.setSlotNumMap(innerSlotPackage);

        FrameLayout.LayoutParams innerCalViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.staticLayer.addView(innerCalView,innerCalViewParams);

        staticLayer.setVisibility(VISIBLE);
        this.addView(staticLayer, stcPageParams);
    }

    private void updateInnerCalendarPackage(){
        TimeSlotPackage slotsInfo = dayViewBody.getTimeSlotPackage();
        innerSlotPackage.clear();

        for (WrapperTimeSlot wrapper:slotsInfo.realSlots
                ) {
            String strDate = innerSlotPackage.slotFmt.format(new Date(wrapper.getTimeSlot().getStartTime()));
            innerSlotPackage.add(strDate);
        }
        innerCalView.refreshSlotNum();
    }

//    public void enableTimeSlot(boolean draggable){
//        isTimeslotEnable = true;
//        super.dayViewBody.enableTimeSlot(draggable);
//        //staticLayer become visible
//        staticLayer.setVisibility(VISIBLE);
//    }
//
//    public void disableTimeSlot(){
//        isTimeslotEnable = false;
//        super.dayViewBody.disableTimeSlot();
//        //static Layer become visible
//        staticLayer.setVisibility(VISIBLE);
//    }

    public void addTimeSlot(ITimeTimeSlotInterface slotInfo){
        super.dayViewBody.addTimeSlot(slotInfo);
        updateInnerCalendarPackage();
    }

    public void addTimeSlot(WrapperTimeSlot wrapperTimeSlot){
        super.dayViewBody.addTimeSlot(wrapperTimeSlot);
        updateInnerCalendarPackage();
    }

    public void removeTimeslot(ITimeTimeSlotInterface timeslot){
        super.dayViewBody.getTimeSlotPackage().removeTimeSlot(timeslot);
        updateInnerCalendarPackage();
    }

    public void removeTimeslot(WrapperTimeSlot wrapper){
        super.dayViewBody.getTimeSlotPackage().removeTimeSlot(wrapper);
        updateInnerCalendarPackage();
    }

    public void setITimeCalendarTimeslotViewListener(ITimeCalendarTimeslotViewListener timeCalendarTimeslotViewListener) {
        this.iTimeCalendarListener = timeCalendarTimeslotViewListener;
        super.dayViewBody.setOnTimeSlotListener(onTimeSlotViewBodyInnerListener);
    }

    public void resetTimeSlots(){
        super.dayViewBody.resetTimeSlots();
        innerSlotPackage.numSlotMap.clear();
    }


    // add self needs to passed listener
    DayViewBody.OnViewBodyTimeSlotListener onTimeSlotViewBodyInnerListener = new DayViewBody.OnViewBodyTimeSlotListener() {
        @Override
        public void onAllDayRcdTimeslotClick(long dayBeginMilliseconds) {
            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onAllDayRcdTimeslotClick(dayBeginMilliseconds);
            }
        }

        @Override
        public void onAllDayTimeslotClick(DraggableTimeSlotView draggableTimeSlotView) {
            if (mode == ViewMode.ALL_DAY_CREATE){
                showTimeslotOptionMenu(draggableTimeSlotView, true);
            }

            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onAllDayTimeslotClick(draggableTimeSlotView);
            }
        }

        @Override
        public void onTimeSlotCreate(DraggableTimeSlotView draggableTimeSlotView) {
            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onTimeSlotCreate(draggableTimeSlotView);
            }
        }

        @Override
        public void onTimeSlotClick(DraggableTimeSlotView draggableTimeSlotView) {
            if (mode == ViewMode.NON_ALL_DAY_CREATE){
                showTimeslotOptionMenu(draggableTimeSlotView, false);
            }

//            if (mode == ViewMode.NON_ALL_DAY_SELECT){
//                WrapperTimeSlot wrapperTimeSlot = draggableTimeSlotView.getWrapper();
//                wrapperTimeSlot.setSelected(!wrapperTimeSlot.isSelected());
//                draggableTimeSlotView.updateViewStatus();
//            }

            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onTimeSlotClick(draggableTimeSlotView);
            }
        }

        @Override
        public void onRcdTimeSlotClick(RcdRegularTimeSlotView v) {
            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onRcdTimeSlotClick(v);
            }
        }

        @Override
        public void onTimeSlotDragStart(DraggableTimeSlotView draggableTimeSlotView) {
            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onTimeSlotDragStart(draggableTimeSlotView);
            }
        }

        @Override
        public void onTimeSlotDragging(DraggableTimeSlotView draggableTimeSlotView, MyCalendar curAreaCal, int x, int y, String locationTime) {
            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onTimeSlotDragging(draggableTimeSlotView, curAreaCal, x, y, locationTime);
            }
        }

        @Override
        public void onTimeSlotDragDrop(DraggableTimeSlotView draggableTimeSlotView, long startTime, long endTime) {
            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onTimeSlotDragDrop(draggableTimeSlotView, startTime, endTime);
            }
            updateInnerCalendarPackage();
        }

        @Override
        public void onTimeSlotDragEnd(DraggableTimeSlotView draggableTimeSlotView) {
            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onTimeSlotDragEnd(draggableTimeSlotView);
            }
        }

        @Override
        public void onTimeSlotEdit(DraggableTimeSlotView draggableTimeSlotView) {
            showTimeslotEditDialog(draggableTimeSlotView);
        }

        @Override
        public void onTimeSlotDelete(DraggableTimeSlotView draggableTimeSlotView) {
            if (iTimeCalendarListener != null){
                iTimeCalendarListener.onTimeSlotDelete(draggableTimeSlotView);
            }

            dayViewBody.notifyDataSetChanged();
        }
    };

    private void showTimeslotEditDialog(final DraggableTimeSlotView dgTimeslot){
        final TimeslotEditView timeslotChangeView = new TimeslotEditView(context,dgTimeslot.getTimeslot());
        int padding = DensityUtil.dip2px(context,15);
        timeslotChangeView.setPadding(0,padding,0,0);
        timeslotChangeView.setBackground(getResources().getDrawable(R.drawable.itime_round_corner_bg));

        TextView titleTv = new TextView(getContext());
        titleTv.setPadding(0,padding,0,padding);
        titleTv.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setText(context.getString(R.string.edit_timeslot_title));
        titleTv.setTextColor(ContextCompat.getColor(context,R.color.black));
        titleTv.setTextSize(16);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setView(timeslotChangeView);
        builder.setCustomTitle(titleTv);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.saveStr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                long newStartTime = timeslotChangeView.getCurrentSelectedStartTime();
                dgTimeslot.setNewStartTime(newStartTime);
                iTimeCalendarListener.onTimeSlotEdit(dgTimeslot);
            }
        });
        builder.setNegativeButton(R.string.cancelStr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showTimeslotOptionMenu(final DraggableTimeSlotView dgTimeslot, boolean allday){
        PopupMenu popupMenu = new PopupMenu(getContext(),dgTimeslot);
        popupMenu.getMenuInflater().inflate(
                allday ? R.menu.timelost_allday_popup_menu : R.menu.timelost_popup_menu
                , popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.edit_timeslot){
                    onTimeSlotViewBodyInnerListener.onTimeSlotEdit(dgTimeslot);
                    return true;
                }else if(id == R.id.delete_timeslot){
                    onTimeSlotViewBodyInnerListener.onTimeSlotDelete(dgTimeslot);
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private List<DurationItem> durationData;

    public void setTimeslotDurationItems(List<DurationItem> data){
        this.durationData = data;
        durationBar.setDate(getDurationItemNames());
    }

    private OnTimeslotDurationChangedListener onTimeslotDurationChangedListener;

    public void setOnTimeslotDurationChangedListener(OnTimeslotDurationChangedListener onTimeslotDurationChangedListener) {
        this.onTimeslotDurationChangedListener = onTimeslotDurationChangedListener;
    }

    public interface OnTimeslotDurationChangedListener {
        void onTimeslotDurationChanged(long duration);
    }

    public void setTimeslotDuration(long duration, boolean animate){
        dayViewBody.updateTimeSlotsDuration(duration,false);
    }

    public static class TimeSlotPackage{
        public ArrayList<WrapperTimeSlot> rcdSlots = new ArrayList<>();
        public ArrayList<WrapperTimeSlot> realSlots = new ArrayList<>();

        public void clear(){
            rcdSlots.clear();
            realSlots.clear();
        }

        void removeTimeSlot(WrapperTimeSlot wrapperTimeSlot){
            if (wrapperTimeSlot.isRecommended() && !wrapperTimeSlot.isSelected()){
                rcdSlots.remove(wrapperTimeSlot);
            }else {
                realSlots.remove(wrapperTimeSlot);
            }
        }

        void removeTimeSlot(ITimeTimeSlotInterface itimeTimeSlotInterface){
            for (WrapperTimeSlot wrapper:rcdSlots
                 ) {
                if (wrapper.getTimeSlot() != null && wrapper.getTimeSlot() == itimeTimeSlotInterface){
                    rcdSlots.remove(wrapper);
                    return;
                }
            }

            for (WrapperTimeSlot wrapper:realSlots
                    ) {
                if (wrapper.getTimeSlot() != null && wrapper.getTimeSlot() == itimeTimeSlotInterface){
                    rcdSlots.remove(wrapper);
                    return;
                }
            }
        }
    }

    public static class DurationItem{
        public String showName;
        public long duration;
    }

    private List<String> getDurationItemNames(){
        List<String> list = new ArrayList<>();
        if (durationData != null){
            for (DurationItem item:durationData
                 ) {
                list.add(item.showName);
            }
        }

        return list;
    }

    public void refresh(){
        dayViewBody.refresh();
    }

    public void showTimeslot(){
        dayViewBody.showBodyTimeslot();
    }

    public void hideTimeslot(){
        dayViewBody.hideBodyTimeslot();
    }

    private void showDurationBar(){
        durationBarPlaceholder.setVisibility(VISIBLE);
        durationBar.setVisibility(VISIBLE);
    }

    private void hideDurationBar(){
        durationBarPlaceholder.setVisibility(GONE);
        durationBar.setVisibility(GONE);
    }

    public void setViewMode(ViewMode mode){
        TimeSlotView.mode = mode;

        switch (TimeSlotView.mode){
            case ALL_DAY_CREATE:
                calendarConfig.enableCreateTimeslotAllday();
                showDurationBar();
                break;
            case NON_ALL_DAY_CREATE:
                calendarConfig.enableCreateTimeslotRegular();
                showDurationBar();
                break;
            case ALL_DAY_SELECT:
                calendarConfig.enableViewTimeslotAllday();
                hideDurationBar();
                break;
            case NON_ALL_DAY_SELECT:
                calendarConfig.enableViewTimeslotRegular();
                hideDurationBar();
                break;
        }

        dayViewBody.notifyDataSetChanged();
    }


}
