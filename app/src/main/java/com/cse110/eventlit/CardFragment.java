package com.cse110.eventlit;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cse110.eventlit.db.Event;
import com.cse110.eventlit.db.Organization;
import com.cse110.eventlit.db.RSVP;
import com.cse110.eventlit.db.User;
import com.cse110.utils.EventUtils;
import com.cse110.utils.OrganizationUtils;
import com.cse110.utils.UserUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by rahulsabnis on 2/22/17.
 */

public class CardFragment extends android.support.v4.app.Fragment {

    ArrayList<Event> allEvents = new ArrayList<>();
    ArrayList<Event> listEvents = new ArrayList<>();

    RecyclerView MyRecyclerView;
    MyAdapter adapter;

    private HashMap<String, RSVP> events;

    private boolean mOrganizerStatus = false;

    private String type;

    private User user;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_card, container, false);
        MyRecyclerView = (RecyclerView) view.findViewById(R.id.cardView);
        MyRecyclerView.setHasFixedSize(true);
        LinearLayoutManager MyLayoutManager = new LinearLayoutManager(getActivity());
        MyLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        adapter = new MyAdapter(listEvents);

        user = UserUtils.getCurrentUser();

        events = user.getEventsFollowing();

        Bundle pageType = getArguments();
        type = pageType.getString("type");

        mOrganizerStatus = getArguments().getBoolean("organizer");


        if (type.equals("feed")) {
            // TODO: Only get subscribed events instead of all events
            final Task<ArrayList<Event>> eventsFollowingTask;
            final Task<ArrayList<Event>> eventsFromOrgsTask;

            Tasks.whenAll(
                    eventsFollowingTask = UserUtils.getEventsFollowing(),
                    eventsFromOrgsTask = UserUtils.getEventsForOrgs()
            ).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        allEvents.clear();
                        allEvents.addAll(eventsFollowingTask.getResult());
                        allEvents.addAll(eventsFromOrgsTask.getResult());

                        // Removes Duplicate events, Sorts by start date
                        Set<Event> removeDups = new TreeSet<>(allEvents);
                        listEvents.clear();
                        listEvents.addAll(removeDups);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        } else {
            final Task<ArrayList<Event>> allEventsTask;
            Tasks.whenAll(allEventsTask = EventUtils.getAllEvents()).addOnCompleteListener(
                    new OnCompleteListener<Void>() {
                        @Override public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                allEvents.clear();
                                allEvents.addAll(allEventsTask.getResult());

                                // Removes Duplicate events, Sorts by start date
                                Set<Event> removeDups = new TreeSet<>(allEvents);
                                listEvents.clear();
                                listEvents.addAll(removeDups);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
            );
        }

        MyRecyclerView.setAdapter(adapter);
        MyRecyclerView.setLayoutManager(MyLayoutManager);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    public void filterBy(ArrayList<String> categories) {
        listEvents.clear();
        for (Event e : allEvents) {
            if (categories.contains(e.getCategory()))
                listEvents.add(e);
        }
        adapter.notifyDataSetChanged();
    }

    public void sortBy(Comparator<Event> eventComparator) {
        Collections.sort(listEvents, eventComparator);
        adapter.notifyDataSetChanged();
    }


    public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private ArrayList<Event> list;
        private int numAttendees;

        public MyAdapter(ArrayList<Event> Data) {
            list = Data;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent,int viewType) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycle_items, parent, false);
            MyViewHolder holder = new MyViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {

            final Event e = list.get(position);
            String category = e.getCategory();
            final String eventName = e.getTitle();

            user = UserUtils.getCurrentUser();
            events = user.getEventsFollowing();

            if (events.containsKey(e.getEventid())) {
                RSVP st = events.get(e.getEventid());
                RSVP.Status statusOfRSVP = st.getRsvpStatus();
                if (statusOfRSVP == RSVP.Status.GOING) {
                    holder.goingButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.goingColor, null));
                    holder.interestedButton.setBackgroundColor(Color.GRAY);
                    holder.notGoingButton.setBackgroundColor(Color.GRAY);
                } else if (statusOfRSVP == RSVP.Status.INTERESTED) {
                    holder.interestedButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.interestedColor, null));
                    holder.goingButton.setBackgroundColor(Color.GRAY);
                    holder.notGoingButton.setBackgroundColor(Color.GRAY);
                } else if (statusOfRSVP == RSVP.Status.NOT_GOING) {
                    holder.notGoingButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.notGoingColor, null));
                    holder.goingButton.setBackgroundColor(Color.GRAY);
                    holder.interestedButton.setBackgroundColor(Color.GRAY);
                }

            }

            holder.timeTextView.setText(String.format("%s",
                    e.formattedStartTime("hh:mma")));
            holder.locationTextView.setText(list.get(position).getLocation());
            holder.categoriesTextView.setText(category);
            holder.eventNameTextView.setText(eventName);
            holder.dateTextView.setText(e.formattedStartTime("LLL\nd"));
            holder.eventIdTextView.setText(e.getEventid());
            holder.orgIdTextView.setText(e.getOrgid());

            holder.setDescription(e.getDescription());
            holder.setNumAttendees(e.getAttendees());
            holder.setStartDate(e.getStartDate());
            holder.setEndDate(e.getEndDate());

            OrganizationUtils.loadOrgs().addOnCompleteListener(new OnCompleteListener<ArrayList<Organization>>() {
                @Override
                public void onComplete(@NonNull Task<ArrayList<Organization>> task) {
                    Organization org = OrganizationUtils.orgFromId(e.getOrgid());
                    holder.orgNameTextView.setText(org.getName());
                }
            });

            if (mOrganizerStatus && user.getOrgsManaging().contains(e.getOrgid())) {
                holder.goingButton.setVisibility(View.INVISIBLE);
                holder.interestedButton.setVisibility(View.INVISIBLE);
                holder.notGoingButton.setVisibility(View.INVISIBLE);

                // Sets size of buttons to 0
                holder.goingButton.setLayoutParams(new LinearLayout.LayoutParams(0,0,0));
                holder.interestedButton.setLayoutParams(new LinearLayout.LayoutParams(0,0,0));
                holder.notGoingButton.setLayoutParams(new LinearLayout.LayoutParams(0,0,0));
                holder.line1.setLayoutParams(new LinearLayout.LayoutParams(0,0,0));
                holder.line2.setLayoutParams(new LinearLayout.LayoutParams(0,0,0));

                // Show Hosting Bar
                holder.hostingBar.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT, (float)3.4));
                holder.hostingBar.setVisibility(View.VISIBLE);

            } else {
                holder.goingButton.setVisibility(View.VISIBLE);
                holder.interestedButton.setVisibility(View.VISIBLE);
                holder.notGoingButton.setVisibility(View.VISIBLE);

                // Shows RSVP Buttons
                holder.goingButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,1));

                holder.interestedButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,1));

                holder.notGoingButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,1));


                holder.line1.setLayoutParams(new LinearLayout.LayoutParams(
                        0,LinearLayout.LayoutParams.MATCH_PARENT, (float)0.2));
                holder.line2.setLayoutParams(new LinearLayout.LayoutParams(
                        0,LinearLayout.LayoutParams.MATCH_PARENT, (float)0.2));

                // Show Hosting Bar
                holder.hostingBar.setLayoutParams(new LinearLayout.LayoutParams(0,0,0));
                holder.hostingBar.setVisibility(View.INVISIBLE);

            }

            holder.goingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RSVP status = new RSVP(e.getOrgid(), e.getEventid(), RSVP.Status.GOING);
                    int modBy = UserUtils.addEventsFollowing(e.getEventid(), status);
                    holder.goingButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.goingColor, null));
                    holder.interestedButton.setBackgroundColor(Color.GRAY);
                    holder.notGoingButton.setBackgroundColor(Color.GRAY);

                    EventUtils.modAttendees(e.getOrgid(), e.getEventid(), modBy);
                }
            });

            holder.interestedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RSVP status = new RSVP(e.getOrgid(), e.getEventid(), RSVP.Status.INTERESTED);
                    int modBy = UserUtils.addEventsFollowing(e.getEventid(), status);
                    holder.interestedButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.interestedColor, null));
                    holder.goingButton.setBackgroundColor(Color.GRAY);
                    holder.notGoingButton.setBackgroundColor(Color.GRAY);

                    EventUtils.modAttendees(e.getOrgid(), e.getEventid(), modBy);
                }
            });

            holder.notGoingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RSVP status = new RSVP(e.getOrgid(), e.getEventid(), RSVP.Status.NOT_GOING);
                    int modBy = UserUtils.addEventsFollowing(e.getEventid(), status);
                    holder.notGoingButton.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.notGoingColor, null));
                    holder.goingButton.setBackgroundColor(Color.GRAY);
                    holder.interestedButton.setBackgroundColor(Color.GRAY);

                    EventUtils.modAttendees(e.getOrgid(), e.getEventid(), modBy);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }



    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public AppCompatTextView locationTextView;
        public AppCompatTextView timeTextView;
        public AppCompatTextView categoriesTextView;
        public AppCompatTextView eventNameTextView;
        public AppCompatTextView dateTextView;
        public AppCompatTextView orgNameTextView;

        public AppCompatButton goingButton;
        public AppCompatButton interestedButton;
        public AppCompatButton notGoingButton;
        public AppCompatButton hostingBar;

        public AppCompatTextView orgIdTextView;
        public AppCompatTextView eventIdTextView;

        public View line1;
        public View line2;

        private String description;
        private int numAttendees;

        private long startDate;
        private long endDate;

        public MyViewHolder(View v) {
            super(v);
            timeTextView = (AppCompatTextView) v.findViewById(R.id.time);
            locationTextView = (AppCompatTextView) v.findViewById(R.id.location);
            categoriesTextView = (AppCompatTextView) v.findViewById(R.id.categories);
            eventNameTextView = (AppCompatTextView) v.findViewById(R.id.eventName);
            dateTextView = (AppCompatTextView) v.findViewById(R.id.dateView);
            orgNameTextView = (AppCompatTextView) v.findViewById(R.id.orgName);

            goingButton = (AppCompatButton) v.findViewById(R.id.going);
            interestedButton = (AppCompatButton) v.findViewById(R.id.interested);
            notGoingButton = (AppCompatButton) v.findViewById(R.id.notGoing);
            hostingBar = (AppCompatButton) v.findViewById(R.id.hostingBar);

            line1 = (View) v.findViewById(R.id.line1);
            line2 = (View) v.findViewById(R.id.line2);

            orgIdTextView = (AppCompatTextView) v.findViewById(R.id.orgId);
            eventIdTextView = (AppCompatTextView) v.findViewById(R.id.eventId);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle extras = new Bundle();

                    String eventid = eventIdTextView.getText().toString();
                    String orgId = orgIdTextView.getText().toString();

                    User user = UserUtils.getCurrentUser();

                    Intent openDetailedView = null;

                    if (mOrganizerStatus && user.getOrgsManaging().contains(orgId)) {
                        openDetailedView = new Intent(getActivity(), OrganizerDetailedEventActivity.class);
                    } else {
                        openDetailedView = new Intent(getActivity(), StudentDetailedEventActivity.class);
                        if (events.containsKey(eventid)) {
                            RSVP.Status stat = events.get(eventid).getRsvpStatus();
                            if (stat == RSVP.Status.GOING) {
                                extras.putInt("RSVP", 1);
                            } else if (stat == RSVP.Status.INTERESTED) {
                                extras.putInt("RSVP", 2);
                            } else if (stat == RSVP.Status.NOT_GOING) {
                                extras.putInt("RSVP", 3);
                            }
                        } else {
                            extras.putInt("RSVP", 0);
                        }
                    }

                    extras.putBoolean("organizer", mOrganizerStatus);
                    extras.putLong("startDate", startDate);
                    extras.putLong("endDate", endDate);
                    extras.putString("location", locationTextView.getText().toString());
                    extras.putString("category", categoriesTextView.getText().toString());
                    extras.putString("eventName", eventNameTextView.getText().toString());
                    extras.putString("description", description);
                    extras.putInt("num_attending", numAttendees);
                    extras.putString("org_name", orgNameTextView.getText().toString());
                    extras.putString("org_id", orgId);
                    extras.putString("event_id", eventid);
                    extras.putString("type", type);
                    openDetailedView.putExtras(extras);
                    startActivity(openDetailedView);
                    getActivity().finish();
                }
            });
        }

        public void setNumAttendees(int numAttendees) {
            this.numAttendees = numAttendees;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setStartDate(long eventDateStart) {
            startDate = eventDateStart;
        }

        public void setEndDate(long eventDateEnd) {
            endDate = eventDateEnd;
        }
    }
}
