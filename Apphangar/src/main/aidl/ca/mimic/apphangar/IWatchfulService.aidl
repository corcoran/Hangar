package ca.mimic.apphangar;

interface IWatchfulService {
    void clearTasks();
    void runScan();
    void destroyNotification();
    void buildTasks();

}