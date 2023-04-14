package edu.wisc.ece.pinpoint.data;

import java.util.Comparator;
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

    public void addAll(ActivityList activityList) {
        activity.addAll(activityList.getActivity());
    }

    public void sort() {
        activity.sort(new SortByTimestamp());
    }

    private static class SortByTimestamp implements Comparator<ActivityItem> {
        @Override
        public int compare(ActivityItem itemA, ActivityItem itemB) {
            return itemA.getTimestamp().compareTo(itemB.getTimestamp());
        }
    }
}
