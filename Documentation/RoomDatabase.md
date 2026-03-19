# Database Explanation

## Task.java
This class of object is used to store each task created by the user.

    @Entity(tableName = "tasks")

    public class Task {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public String location;
    public long startTime;
    public long endTime;
    public boolean isCompleted;
    public String uid;
}

@entity creates a new table called tasks in the SQLite database, and the fields that follow become the columns.

- id - Auto-generated unique identifier for each task
- title - Task name entered by the user
- description - Extra details/description
- location - location of task
- startTime - start time of task (Unix timestamp)
- endTime - end time of task (Unix Timestamp) (We are maybe not doing this?)
- isCompleted - Boolean for task completion
- uid - unique ID which is required for exporting to an ics file

Unix timestamps are easier to query with rather than storing formatted date strings

## TaskDao.java
DAO stands for Data Access Object. It is an interface so you don't need to write any of the SQL code for insertion and such, Room does it automatically using the ids created for each task.
There are two raw SQL queries in here, for showing tasks for the day you clicked on the calendar, and for listing all of your tasks sorted by their start time.

    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :start AND :end ORDER BY startTime ASC")
    LiveData<List<Task>> getTasksForDay(long start, long end);

    @Query("SELECT * FROM tasks ORDER BY startTime ASC")
    LiveData<List<Task>> getAllTasks();

LiveData<List<Task>> is used as the return type, which is how the UI updates automatically when a new task is added.
LiveData is an observable container so anything observing it gets notified when the data in the database changes.
This means that when a task is inserted the UI updates automatically without any manual effort.
## AppDatabase.java
    @Database(entities = {Task.class}, version = 2)
This tells Room which tables exist and the current version. The version needs to be incremented every time the table structure is changed so that Room does not crash on devices with the old version installed.
Not sure how we are going to handle this with our app, or if we even need some way of migrating the data between app changes or if that's just unnecessary work.

    public abstract TaskDao taskDao();
Room generates the actual implementation of this at compile time. 

    public abstract TaskDao taskDao();
    // make sure only one database instance is running at a time
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "task_database"
                    )
                    .build();
        }
        return instance;
    }

In this, instance starts as null. When getInstance is called for the first time it creates the database. 
When it is called after that it returns the existing instance of the database.
It is synchronized so two databases cannot be created if two threads call this at the same time somehow.

The database itself is stored locally at "/data/data/com.example.myapplication/databases/task_database".
This requires root access to see and is deleted if the app is uninstalled, so maybe we should make a backup feature?

## TaskRepository.java
    public TaskRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        taskDao = db.taskDao();
    }
This returns the database instance and also grabs the DAO from it. This is the only place that the app interacts with the database directly.

    public void insert(Task task) {
        Executors.newSingleThreadExecutor().execute(() -> taskDao.insert(task));
    }
For all of the write operations, Room does not allow them to run on the main thread as it could cause the UI to freeze.
The "Executors.newSingleThreadExecutor()" creates a new background thread to run the insert and then finishes.
The read operations do not need this precaution since their threads are handled by the LiveData class automatically.
## TaskViewModel.java
The ViewModel bridges the UI activities and the repository. Activities do not interact with the repository directly.
public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;

        public TaskViewModel(Application application) {
            super(application);
            repository = new TaskRepository(application);
        }
    
        // activities talk to this rather than the repository directly
        // and it also isnt affected by screen rotation
        public LiveData<List<Task>> getAllTasks() {
            return repository.getAllTasks();
        }
    
        public LiveData<List<Task>> getTasksForDay(long start, long end) {
            return repository.getTasksForDay(start, end);
        }
    
        public void insert(Task task) {
            repository.insert(task);
        }
    
        public void delete(Task task) {
            repository.delete(task);
        }
    
        public void update(Task task) {
            repository.update(task);
        }
    }
The ViewModel is important for keeping data when the screen is rotated. 
When rotation happens, activities are all destroyed and recreated. 
The database query would restart of every rotation without the ViewModel.
With it, the data stays and reattaches to the new Activity that is created.

## MainActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialCardView cardCalendar = findViewById(R.id.cardCalendar);
        cardCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
        });

        MaterialCardView cardTodo = findViewById(R.id.cardTodo);
        cardTodo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TodoActivity.class);
            startActivity(intent);
        });
    }
This file is for the main screen and is fairly simple. Its purpose right now is just to select between the calendar and todo pages.
findViewById looks into the XML file for the card with the given ID.
Intent is how android knows you have the intent to open the other screen, and then startActivity triggers the navigation.
The listeners here watch for clicks on the two cards.

## CalendarActivity

## TodoActivity

## IcsExporter