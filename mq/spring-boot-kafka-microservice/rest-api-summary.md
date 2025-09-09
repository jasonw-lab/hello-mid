# REST APIs in test-suspicious-activity.sh

The script uses a single REST API endpoint:

## GET /api/pageviews/simulate/{userId}/{count}?delayMs={delayMs}

**Parameters:**
- **userId** (path variable): User ID for simulating page views
- **count** (path variable): Number of page views to simulate
- **delayMs** (query parameter, optional): Delay in milliseconds between page views (default: 0)

**Examples from the script:**
1. `GET /api/pageviews/simulate/normal-user/50?delayMs=100`
2. `GET /api/pageviews/simulate/suspicious-user/150?delayMs=10`
3. `GET /api/pageviews/simulate/user1/20?delayMs=100`
4. `GET /api/pageviews/simulate/user2/30?delayMs=100`
5. `GET /api/pageviews/simulate/user3/120?delayMs=10`

**Purpose:**
This endpoint simulates page views to test the system's ability to detect suspicious user activity based on the number of page views within a time wi
ndow.