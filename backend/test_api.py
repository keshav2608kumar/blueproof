import sys
import os
import time

# Add backend directory to sys.path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_backend_end_to_end():
    run_id = int(time.time() * 1000)
    student_id = f"STU_{run_id}"
    device_id = f"DEV_PHONE_{run_id}"

    print("=== TEST 1: Health Check ===")
    res = client.get("/health")
    assert res.status_code == 200, f"Health check failed: {res.text}"
    assert res.json()["status"] == "ok"
    print("PASSED: Health check OK")

    print("\n=== TEST 2: Register Student ===")
    reg_payload = {
        "studentId": student_id,
        "name": f"Alice Test {run_id}",
        "deviceId": device_id
    }
    res = client.post("/students/register", json=reg_payload)
    assert res.status_code in [200, 201], f"Register student failed: {res.text}"
    data = res.json()
    assert data["studentId"] == student_id
    assert len(data["enrollmentKey"]) == 64
    alice_key = data["enrollmentKey"]
    print(f"PASSED: Student registered, enrollment key: {alice_key[:8]}...")

    print("\n=== TEST 3: Start Session ===")
    session_payload = {
        "name": f"CS101 Intro {run_id}",
        "teacherName": "Dr. Alexander Vance",
        "durationMinutes": 30,
        "rssiThreshold": -75
    }
    res = client.post("/sessions/start", json=session_payload)
    assert res.status_code in [200, 201], f"Start session failed: {res.text}"
    session_data = res.json()
    session_id = session_data["sessionId"]
    challenge = session_data["currentChallenge"]
    assert session_id is not None
    assert challenge is not None
    print(f"PASSED: Session started, id: {session_id}, challenge: {challenge[:8]}...")

    print("\n=== TEST 4: Submit Valid Attendance ===")
    submit_payload = {
        "sessionId": session_id,
        "studentId": student_id,
        "studentName": f"Alice Test {run_id}",
        "deviceId": device_id,
        "rssiMedian": -65,
        "rssiSamples": "[-64, -66, -65, -63, -67]",
        "challengeUsed": challenge,
        "status": "ACCEPTED"
    }
    res = client.post("/attendance/submit", json=submit_payload)
    assert res.status_code in [200, 201], f"Submit attendance failed: {res.text}"
    assert res.json()["status"] == "ACCEPTED"
    print("PASSED: Attendance submitted and verified ACCEPTED")

    print("\n=== TEST 5: Prevent Duplicate Submission in Session ===")
    res_dup = client.post("/attendance/submit", json=submit_payload)
    assert res_dup.status_code in [400, 409] or "duplicate" in res_dup.text.lower() or "already" in res_dup.text.lower(), f"Duplicate allowed! {res_dup.text}"
    print("PASSED: Duplicate attendance rejected as expected")

    print("\n=== TEST 5b: Anti-Proxy Test - Reject Same Phone Reused by Another Student ===")
    bob_id = f"STU_BOB_{run_id}"
    proxy_payload = {
        "sessionId": session_id,
        "studentId": bob_id,
        "studentName": f"Bob Proxy {run_id}",
        "deviceId": device_id,  # SAME physical device as Alice
        "rssiMedian": -66,
        "rssiSamples": "[-65, -67, -66, -64, -68]",
        "challengeUsed": challenge,
        "status": "ACCEPTED"
    }
    res_proxy = client.post("/attendance/submit", json=proxy_payload)
    assert res_proxy.status_code == 400 and "proxy" in res_proxy.text.lower(), f"Proxy attack was NOT rejected! {res_proxy.text}"
    print("PASSED: Anti-proxy verified! Phone reuse attack blocked with 400 Bad Request.")

    print("\n=== TEST 6: Get Session Attendance List ===")
    res_list = client.get(f"/sessions/{session_id}/attendance")
    assert res_list.status_code == 200
    records = res_list.json()
    assert len(records) >= 1
    assert records[0]["studentId"] == student_id
    print(f"PASSED: Retrieved {len(records)} record(s) for session")

    print("\n=== TEST 7: Get Student Attendance History ===")
    res_hist = client.get(f"/students/{student_id}/history")
    assert res_hist.status_code == 200
    history = res_hist.json()
    assert len(history) >= 1
    print(f"PASSED: Student history retrieved: {len(history)} record(s)")

    print("\n=== TEST 8: Export CSV ===")
    res_csv = client.get(f"/export/csv/{session_id}")
    assert res_csv.status_code == 200
    assert "Student ID" in res_csv.text
    assert student_id in res_csv.text
    print("PASSED: CSV export generated successfully")

    print("\n=== TEST 9: Analytics Summary ===")
    res_analytics = client.get("/analytics/summary")
    assert res_analytics.status_code == 200
    analytics = res_analytics.json()
    assert analytics.get("totalSessions", 0) >= 1
    assert analytics.get("totalAttendanceRecords", 0) >= 1
    print(f"PASSED: Analytics summary: {analytics}")

    print("\n==========================================")
    print("ALL 10 BACKEND TESTS PASSED SUCCESSFULLY!")
    print("==========================================")

if __name__ == "__main__":
    test_backend_end_to_end()
