package models;

public class Screen {
    private int screenId;
    private String screenName;
    private int capacity;

    public Screen() {}

    public Screen(int screenId, String screenName, int capacity) {
        this.screenId = screenId;
        this.screenName = screenName;
        this.capacity = capacity;
    }

    public int getScreenId() { return screenId; }
    public void setScreenId(int screenId) { this.screenId = screenId; }

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) {
        if (screenName == null || screenName.trim().isEmpty()) {
            throw new IllegalArgumentException("Screen name cannot be empty");
        }
        this.screenName = screenName.trim();
    }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    @Override
    public String toString() {
        return screenName + " (" + capacity + " seats)";
    }
}