import csv
import io
import secrets
import time
import uuid
from typing import List, Optional

from fastapi import Depends, FastAPI, HTTPException, Response, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session as DBSession

from database import Base, engine, get_db
from models import (
    AnalyticsSummaryResponse,
    AttendanceRecord,
    AttendanceResponse,
    AttendanceSubmitRequest,
    AuditLog,
    AuditLogResponse,
    Session as SessionModel,
    SessionEndRequest,
    SessionResponse,
    SessionStartRequest,
    Student,
    StudentRegisterRequest,
    StudentResponse,
)

# Initialize database tables
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="BlueProof Attendance Backend",
    version="1.0.0",
    description="Backend API for BlueProof BLE Attendance System",
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "service": "BlueProof Backend",
        "timestamp": int(time.time() * 1000)
    }


# ==========================================
# Student Endpoints
# ==========================================

@app.post(
    "/students/register",
    response_model=StudentResponse,
    status_code=status.HTTP_201_CREATED,
    tags=["Students"]
)
def register_student(
    request: StudentRegisterRequest,
    db: DBSession = Depends(get_db)
):
    existing = db.query(Student).filter(Student.student_id == request.student_id).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Student ID '{request.student_id}' is already registered."
        )

    enrollment_key = request.enrollment_key or secrets.token_hex(32)
    student = Student(
        student_id=request.student_id,
        name=request.name,
        device_id=request.device_id,
        enrollment_key=enrollment_key,
        enrolled_at=int(time.time() * 1000),
        is_active=True
    )
    db.add(student)

    audit = AuditLog(
        timestamp=int(time.time() * 1000),
        event_type="STUDENT_ENROLLED",
        student_id=student.student_id,
        details=f"Student {student.name} ({student.student_id}) enrolled with device {student.device_id}"
    )
    db.add(audit)
    db.commit()
    db.refresh(student)
    return student


@app.get(
    "/students/{student_id}",
    response_model=StudentResponse,
    tags=["Students"]
)
def get_student(
    student_id: str,
    db: DBSession = Depends(get_db)
):
    student = db.query(Student).filter(Student.student_id == student_id).first()
    if not student:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Student with ID '{student_id}' not found."
        )
    return student


# ==========================================
# Session Endpoints
# ==========================================

@app.post(
    "/sessions/start",
    response_model=SessionResponse,
    status_code=status.HTTP_201_CREATED,
    tags=["Sessions"]
)
def start_session(
    request: SessionStartRequest,
    db: DBSession = Depends(get_db)
):
    session_id = request.session_id or str(uuid.uuid4())
    existing = db.query(SessionModel).filter(SessionModel.session_id == session_id).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Session with ID '{session_id}' already exists."
        )

    now = int(time.time() * 1000)
    challenge = request.initial_challenge or secrets.token_hex(32)
    challenge_expiry = now + 30_000  # 30 seconds

    session = SessionModel(
        session_id=session_id,
        name=request.name,
        teacher_name=request.teacher_name,
        start_time=now,
        end_time=None,
        duration_minutes=request.duration_minutes,
        status="ACTIVE",
        rssi_threshold=request.rssi_threshold,
        current_challenge=challenge,
        challenge_expires_at=challenge_expiry
    )
    db.add(session)

    audit = AuditLog(
        timestamp=now,
        event_type="SESSION_STARTED",
        session_id=session_id,
        details=f"Session '{session.name}' started by {session.teacher_name}"
    )
    db.add(audit)
    db.commit()
    db.refresh(session)
    return session


@app.post(
    "/sessions/{session_id}/end",
    response_model=SessionResponse,
    tags=["Sessions"]
)
def end_session(
    session_id: str,
    request: Optional[SessionEndRequest] = None,
    db: DBSession = Depends(get_db)
):
    session = db.query(SessionModel).filter(SessionModel.session_id == session_id).first()
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Session '{session_id}' not found."
        )

    now = int(time.time() * 1000)
    session.status = "ENDED"
    session.end_time = request.end_time if (request and request.end_time) else now

    audit = AuditLog(
        timestamp=now,
        event_type="SESSION_ENDED",
        session_id=session_id,
        details=f"Session '{session.name}' ended"
    )
    db.add(audit)
    db.commit()
    db.refresh(session)
    return session


@app.get(
    "/sessions/{session_id}",
    response_model=SessionResponse,
    tags=["Sessions"]
)
def get_session(
    session_id: str,
    db: DBSession = Depends(get_db)
):
    session = db.query(SessionModel).filter(SessionModel.session_id == session_id).first()
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Session '{session_id}' not found."
        )
    return session


@app.get(
    "/sessions",
    response_model=List[SessionResponse],
    tags=["Sessions"]
)
def list_sessions(
    db: DBSession = Depends(get_db)
):
    return db.query(SessionModel).order_by(SessionModel.start_time.desc()).all()


# ==========================================
# Attendance Endpoints
# ==========================================

@app.post(
    "/attendance/submit",
    response_model=AttendanceResponse,
    status_code=status.HTTP_201_CREATED,
    tags=["Attendance"]
)
def submit_attendance(
    request: AttendanceSubmitRequest,
    db: DBSession = Depends(get_db)
):
    session = db.query(SessionModel).filter(SessionModel.session_id == request.session_id).first()
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Session '{request.session_id}' not found."
        )

    student = db.query(Student).filter(Student.student_id == request.student_id).first()
    student_name = request.student_name or (student.name if student else request.student_id)

    # 1. Anti-Proxy Safeguard: Reject if physical device was already used by another student in this session
    if request.device_id:
        conflict_device = db.query(AttendanceRecord).filter(
            AttendanceRecord.session_id == request.session_id,
            AttendanceRecord.device_id == request.device_id,
            AttendanceRecord.student_id != request.student_id,
            AttendanceRecord.status == "ACCEPTED"
        ).first()
        if conflict_device:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Proxy attempt rejected: Physical phone already used by {conflict_device.student_name} ({conflict_device.student_id}) in this session."
            )

    # 2. Check for duplicate submission for this student and session
    existing_record = db.query(AttendanceRecord).filter(
        AttendanceRecord.student_id == request.student_id,
        AttendanceRecord.session_id == request.session_id
    ).first()

    now = int(time.time() * 1000)
    check_in_time = request.check_in_time or now

    if existing_record:
        if existing_record.status == "ACCEPTED":
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail=f"Attendance already recorded and accepted for student '{request.student_id}' in session '{request.session_id}'."
            )

        # Update existing rejected/pending record
        existing_record.student_name = student_name
        existing_record.check_in_time = check_in_time
        existing_record.rssi_median = request.rssi_median
        existing_record.rssi_samples = request.rssi_samples
        existing_record.device_id = request.device_id
        existing_record.status = request.status
        existing_record.rejection_reason = request.rejection_reason
        existing_record.challenge_used = request.challenge_used
        existing_record.synced_to_backend = True

        audit = AuditLog(
            timestamp=now,
            event_type=f"CHECKIN_UPDATED_{request.status}",
            session_id=request.session_id,
            student_id=request.student_id,
            details=f"Attendance updated for {student_name}: {request.status}"
        )
        db.add(audit)
        db.commit()
        db.refresh(existing_record)
        return existing_record

    record = AttendanceRecord(
        student_id=request.student_id,
        session_id=request.session_id,
        student_name=student_name,
        check_in_time=check_in_time,
        rssi_median=request.rssi_median,
        rssi_samples=request.rssi_samples,
        device_id=request.device_id,
        status=request.status,
        rejection_reason=request.rejection_reason,
        challenge_used=request.challenge_used,
        synced_to_backend=True
    )
    db.add(record)

    event_type = "CHECKIN_ACCEPTED" if request.status == "ACCEPTED" else "CHECKIN_REJECTED"
    audit = AuditLog(
        timestamp=now,
        event_type=event_type,
        session_id=request.session_id,
        student_id=request.student_id,
        details=f"Attendance submitted for {student_name}: {request.status} (Reason: {request.rejection_reason or 'None'})"
    )
    db.add(audit)
    db.commit()
    db.refresh(record)
    return record


@app.get(
    "/sessions/{session_id}/attendance",
    response_model=List[AttendanceResponse],
    tags=["Attendance"]
)
def get_session_attendance(
    session_id: str,
    db: DBSession = Depends(get_db)
):
    return db.query(AttendanceRecord).filter(
        AttendanceRecord.session_id == session_id
    ).order_by(AttendanceRecord.check_in_time.desc()).all()


@app.get(
    "/students/{student_id}/history",
    response_model=List[AttendanceResponse],
    tags=["Attendance"]
)
def get_student_history(
    student_id: str,
    db: DBSession = Depends(get_db)
):
    return db.query(AttendanceRecord).filter(
        AttendanceRecord.student_id == student_id
    ).order_by(AttendanceRecord.check_in_time.desc()).all()


# ==========================================
# Analytics & Export Endpoints
# ==========================================

@app.get(
    "/analytics/summary",
    response_model=AnalyticsSummaryResponse,
    tags=["Analytics"]
)
def get_analytics_summary(
    db: DBSession = Depends(get_db)
):
    total_sessions = db.query(SessionModel).count()
    active_sessions = db.query(SessionModel).filter(SessionModel.status == "ACTIVE").count()
    total_students = db.query(Student).count()
    total_attendance_records = db.query(AttendanceRecord).count()
    accepted_records = db.query(AttendanceRecord).filter(AttendanceRecord.status == "ACCEPTED").count()
    rejected_records = db.query(AttendanceRecord).filter(AttendanceRecord.status == "REJECTED").count()

    rate = (float(accepted_records) / float(total_attendance_records) * 100.0) if total_attendance_records > 0 else 0.0

    return AnalyticsSummaryResponse(
        total_sessions=total_sessions,
        active_sessions=active_sessions,
        total_students=total_students,
        total_attendance_records=total_attendance_records,
        accepted_records=accepted_records,
        rejected_records=rejected_records,
        overall_attendance_rate=round(rate, 2)
    )


@app.get(
    "/export/csv/{session_id}",
    tags=["Export"]
)
def export_session_attendance_csv(
    session_id: str,
    db: DBSession = Depends(get_db)
):
    session = db.query(SessionModel).filter(SessionModel.session_id == session_id).first()
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Session '{session_id}' not found."
        )

    records = db.query(AttendanceRecord).filter(
        AttendanceRecord.session_id == session_id
    ).order_by(AttendanceRecord.check_in_time.asc()).all()

    output = io.StringIO()
    writer = csv.writer(output)

    writer.writerow([
        "Student ID",
        "Student Name",
        "Session ID",
        "Check-In Time (ms)",
        "Status",
        "RSSI Median (dBm)",
        "RSSI Samples",
        "Rejection Reason",
        "Challenge Used"
    ])

    for r in records:
        writer.writerow([
            r.student_id,
            r.student_name,
            r.session_id,
            r.check_in_time,
            r.status,
            r.rssi_median,
            r.rssi_samples,
            r.rejection_reason or "",
            r.challenge_used
        ])

    csv_data = output.getvalue()
    return Response(
        content=csv_data,
        media_type="text/csv",
        headers={
            "Content-Disposition": f"attachment; filename=attendance_{session_id}.csv"
        }
    )
