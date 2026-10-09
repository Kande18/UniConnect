const app = require('../server');
const { query } = require('../config/db');

async function runAllTests() {
  const PORT = 5099;
  const server = app.listen(PORT);
  const baseUrl = `http://localhost:${PORT}/api`;
  let passed = 0;
  let failed = 0;

  function assert(condition, testName) {
    if (condition) {
      console.log(`  ✅ PASS: ${testName}`);
      passed++;
    } else {
      console.error(`  ❌ FAIL: ${testName}`);
      failed++;
    }
  }

  async function api(method, path, body = null, token = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(`${baseUrl}${path}`, opts);
    let data;
    try {
      data = await res.json();
    } catch {
      data = null;
    }
    return { status: res.status, data };
  }

  console.log('\n======================================================');
  console.log('  UNICONNECT BACKEND API MASTER TEST RUNNER');
  console.log('======================================================\n');

  try {
    // 1. Health check
    console.log('[1] Health Check Endpoint:');
    const health = await api('GET', '/health');
    assert(health.status === 200, 'GET /api/health returns 200');
    assert(health.data.data.status === 'UP', 'Service status is UP');
    assert(health.data.data.database === 'CONNECTED', 'Database is CONNECTED');

    // 2. Authentication
    console.log('\n[2] Authentication & Token Generation:');
    const studentAuth = await api('POST', '/auth/student-login', {
      studentNumber: '202400123',
      password: 'Password123!'
    });
    assert(studentAuth.status === 200, 'Student login returns 200');
    const studentToken = studentAuth.data.data.token;
    assert(!!studentToken, 'Student JWT token generated');
    assert(studentAuth.data.data.user.role === 'STUDENT', 'User role is STUDENT');

    const lecturerAuth = await api('POST', '/auth/lecturer-login', {
      staffId: 'STF-2024-88',
      password: 'LecturerPass2026!'
    });
    assert(lecturerAuth.status === 200, 'Lecturer login returns 200');
    const lecturerToken = lecturerAuth.data.data.token;
    assert(!!lecturerToken, 'Lecturer JWT token generated');
    assert(lecturerAuth.data.data.user.role === 'LECTURER', 'User role is LECTURER');

    // 3. Claim code & Registration
    console.log('\n[3] Student Registration & Claim Code Validation:');
    const badClaim = await api('POST', '/auth/register', {
      studentNumber: '202400876',
      name: 'Alice Zulu',
      programme: 'CS',
      claimCode: 'INVALID-CODE-XXX',
      password: 'Password123!'
    });
    assert(badClaim.status === 400, 'Rejects invalid claim code');

    const validReg = await api('POST', '/auth/register', {
      studentNumber: '202400555',
      name: 'Brian Phiri',
      programme: 'DS',
      claimCode: 'CLAIM-DS-2024',
      password: 'Password123!'
    });
    assert(validReg.status === 201, 'Registers student with valid CLAIM-DS-2024');
    assert(validReg.data.data.studentNumber === '202400555', 'Preserves 9-digit student number');

    // 4. Student Portal
    console.log('\n[4] Student Portal Endpoints:');
    const profile = await api('GET', '/students/profile', null, studentToken);
    assert(profile.status === 200, 'GET /api/students/profile returns 200');
    assert(profile.data.data.studentNumber === '202400123', 'Profile student number matches');
    assert(profile.data.data.groupCapacity === 15, 'Lab group capacity is 15');

    const editProfile = await api('PUT', '/students/profile', {
      name: 'Peculiar Banda',
      programme: 'CS'
    }, studentToken);
    assert(editProfile.status === 200, 'PUT /api/students/profile updates name');

    const myGroup = await api('GET', '/students/my-group', null, studentToken);
    assert(myGroup.status === 200, 'GET /api/students/my-group returns 200');
    assert(myGroup.data.data.groupId === 'G01', 'My group is G01');

    // 5. Groups Endpoint
    console.log('\n[5] Laboratory Cohorts Endpoint:');
    const groups = await api('GET', '/groups', null, studentToken);
    assert(groups.status === 200, 'GET /api/groups returns 200');
    assert(groups.data.data.length === 4, 'Exactly 4 cohorts returned (G01–G04)');

    // 6. Lecturer Dashboard & Roster
    console.log('\n[6] Lecturer Dashboard & Roster:');
    const dash = await api('GET', '/lecturer/dashboard', null, lecturerToken);
    assert(dash.status === 200, 'GET /api/lecturer/dashboard returns 200');
    assert(typeof dash.data.data.metrics.totalStudents === 'number', 'Metrics totalStudents is number');
    assert(dash.data.data.groupSummaries.length === 4, 'Dashboard has 4 group summaries');

    const roster = await api('GET', '/lecturer/roster?query=Peculiar', null, lecturerToken);
    assert(roster.status === 200, 'GET /api/lecturer/roster returns 200');
    assert(roster.data.data.students.length >= 1, 'Roster search finds student');

    // 7. Lecturer Operations (Add, Edit, Delete)
    console.log('\n[7] Lecturer Student Management:');
    const addStudent = await api('POST', '/lecturer/students', {
      studentNumber: '202409876',
      name: 'David Mwape',
      programme: 'CS',
      labGroup: 'G03'
    }, lecturerToken);
    assert(addStudent.status === 201, 'POST /api/lecturer/students creates student');

    const updateStudent = await api('PUT', '/lecturer/students/202409876', {
      name: 'David K. Mwape',
      programme: 'CS',
      labGroup: 'G02'
    }, lecturerToken);
    assert(updateStudent.status === 200, 'PUT /api/lecturer/students updates student');

    const deleteStudent = await api('DELETE', '/lecturer/students/202409876', null, lecturerToken);
    assert(deleteStudent.status === 200, 'DELETE /api/lecturer/students soft deletes student');

    // 8. Transfer Requests & Atomic Approvals
    console.log('\n[8] Transfer Request Review:');
    const reqs = await api('GET', '/lecturer/requests', null, lecturerToken);
    assert(reqs.status === 200, 'GET /api/lecturer/requests returns 200');
    if (reqs.data.data.requests.length > 0) {
      const targetReq = reqs.data.data.requests[0];
      const approveRes = await api('POST', `/lecturer/requests/${targetReq.id}/approve`, null, lecturerToken);
      assert(approveRes.status === 200, `Approved transfer request ${targetReq.id} atomically`);
    }

    // 9. Offline Delta Sync
    console.log('\n[9] Delta Synchronization & Conflict Resolution:');
    const syncPull = await api('GET', '/sync/pull', null, studentToken);
    assert(syncPull.status === 200, 'GET /api/sync/pull returns 200');
    assert(Array.isArray(syncPull.data.data.updatedStudents), 'Updated students returned');
    assert(!!syncPull.data.data.groupCounts, 'Group counts returned');

    const syncPush = await api('POST', '/sync/push', {
      mutations: [
        {
          mutationId: 'MUT-001',
          entity: 'STUDENT',
          entityId: '202400123',
          action: 'UPDATE',
          clientVersion: 999, // higher version -> accepted
          payload: { name: 'Peculiar Banda' }
        }
      ]
    }, studentToken);
    assert(syncPush.status === 200, 'POST /api/sync/push applied mutation');
    assert(syncPush.data.data.applied.includes('MUT-001'), 'Mutation MUT-001 applied');

  } catch (err) {
    console.error('Fatal test error:', err);
    failed++;
  } finally {
    server.close(() => {
      console.log(`\n======================================================`);
      console.log(`  TEST RESULTS: ${passed} Passed, ${failed} Failed`);
      console.log(`======================================================\n`);
      process.exit(failed > 0 ? 1 : 0);
    });
  }
}

runAllTests();
