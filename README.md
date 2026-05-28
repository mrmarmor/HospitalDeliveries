# Hospital Deliveries
This is an Android app for Shiba hospital delivery tasks sent to autonomous robots.
Built for an interview exercise for Deliverz.AI

## What the app does
1. Create a delivery: title (required), description (optional), `from` and `to`
   (dropdown of fixed places: kitchen, warehouse, pharmacy, laundry, emergency, geriatric, eyes, gastro).
2. See the task progress which can be: `CREATED → IDLE → ASSIGNED → IN_PROGRESS → DONE`.
   It could be also `FAILED`(if something went wrong), or `CANCELLED`(if user clicks cancel)
3. The connection state to the robot is displayed in the top bar

## Architecture
MVVM: screen - viewModel - repository - models (using useCases for such project is overkilled)
```
domain/        Models: Task, Location, TaskStatus, TaskRepository interface.
data/local/    Room: TaskDbEntity, TaskDao, AppDatabase.
data/remote/   MockRobot: a single class that simulates a WebSocket.
data/repository/ TaskRepositoryImpl handles data objects + MockRobot. Subscribes to
               robot events for the app lifetime and collects them into the DB.
ui/            1. HomeViewModel contains a single immutable HomeUiState built from
               repository.observeTasks() + repository.connectionState + a form StateFlow.
               2. HomeScreen is the only Compose screen.
HospitalDeliveriesApp contains the dependencies (manual DI).
```

### Data
1. Room is the single source of truth for the UI. The repository writes the local row before sending 
   the command, and indicates every robot status update so illegal updates from the "server" are dropped.
2. The main data entity is `Task`, which is mapped to `TaskDbEntity', and contains the fields:
   Title, Description, From, To, Status, CreatedAt, UpdatedAt.
3. Task possible states are: `CREATED`, `IDLE`, `ASSIGNED`, `IN_PROGRESS`, `DONE`, `FAILED`, `CANCELLED`.

### State management
1. `HomeUiState` is immutable data class, and updates the compose(HomeScreen)
    when its data is changed(task, connectivity, form, errors).
2. Errors surface through the `error` field. the screen shows a snackbar and
   calls `dismissError()` once shown.

### realtime handling
1. `MockRobot.events` is a `SharedFlow<RobotEvent>` that consumed once,
  for the whole application's lifetime, by the repository.
  That prevents duplicate writes when the screen is recreated.
2. `MockRobot.send()` enqueues into an unlimited `Channel`.
   coroutine drains the channel only when the connection state is
  `Connected`, so commands that fired while the connection is off are held and
  re-run on reconnect.
3. The connection function (`mockConnectWithRetry`) and the sender function
  (`runTasks`) are independent coroutines on the app scope , so never blocks the other.

### Error handling
1. Form validation lives in the repository (`require(title\from\to)` checks for
  empty title, identical from/to) and returns on `Result.failure`.
2. The ViewModel translates `Result.failure` into a user friendly error message.

## Tradeoffs and Simplifications
1. No real WebSocket. `MockRobot` simulates a websocket. Swapping to a real websocket means 
   replacing only one class(!) — the `RobotCommand` / `RobotEvent` / `RobotConnectionState` types 
   and everything else stays the same.
2. No DI. Wiring the graph by hand in `HospitalDeliveriesApp` is
   much enough for one screen. Hilt would be a good idea for multi-screen usage.
3. From this reason, navigation mechanism is also to much. The form and the list share one screen, 
   so navigation compose isn't needed.
4. If websocketing is overkilled, we can consider using silent push-notification every 10 seconds
   instead. This will simplify code and reduce battery consumption. 

## What I would improve in production
1. Real data transport. WebSocket + JSON(with serialization) for `RobotCommand` / `RobotEvent`,
   reuse the same sealed-class.
2. Hilt for DI.
3. Appium/JUnit for unit tests in the repository and `MockRobot`.
4. Pagination and performance improvement.
5. Authentication.
6. Security methodologies(ssl pinning, DDos...). Healthcare is a very sensitive field, and must be 
   well-guarded.
7. Crashlytics for crashes and ANR + logs for tracking failures and performance issues on app. 
8. Analytics+telemetrics tracking to get maximum information on users behaviour + robot maintainance.
9. Use-cases layers and MVI architecture for dealing with complicated code.
10. Handling offline-mode(including caching).
11. No need for accessibility - this is only a legal requirement, for mass-production 
   google-play-oriented apps.