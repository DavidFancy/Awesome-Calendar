package david.itime_calendar.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;

import david.itime_calendar.R;
import david.itimecalendar.calendar.listeners.ITimeCalendarMonthAgendaViewListener;
import david.itimecalendar.calendar.listeners.ITimeEventInterface;
import david.itimecalendar.calendar.ui.agendaview.AgendaViewBody;
import david.itimecalendar.calendar.ui.agendaview.MonthAgendaView;

/**
 * Created by yuhaoliu on 8/06/2017.
 */

public class FragmentCalendarAgenda extends Fragment {
    private View root;
    private MonthAgendaView agendaView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_calendar_agenda, container, false);
        initView();
        return root;
    }

    private void initView(){
        agendaView = (MonthAgendaView) root.findViewById(R.id.agenda_view);
        //Set the data source with format of ITimeEventPackageInterface
        //ITimeEventPackageInterface is composed by two parts:
        //  1: regular events. 2: repeated events.
        agendaView.setITimeCalendarMonthAgendaViewListener(new ITimeCalendarMonthAgendaViewListener() {
            @Override
            public void onDateChanged(Date date) {

            }

            @Override
            public void onHeaderFlingDateChanged(Date newestDate) {

            }

            @Override
            public void onEventClick(ITimeEventInterface event) {

            }
        });
    }
}
