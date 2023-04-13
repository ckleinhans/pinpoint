package edu.wisc.ece.pinpoint.data;

import java.util.List;

public class ActivityList {
    private List<ActivityItem> activity;

    public ActivityList() {
    }

    public ActivityList(List<ActivityItem> activity) {
        this.activity = activity;
    }

    public List<ActivityItem> getActivity() {
        return activity;
    }

    public int size() {
        return activity.size();
    }

    public ActivityItem get(int index) {
        return activity.get(index);
    }

    public void add(ActivityItem activityItem) {
        activity.add(activityItem);
    }

}
