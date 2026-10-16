import time
import uuid
from typing import Optional, List
from sqlalchemy import (
    Column,
    Integer,
    BigInteger,
    String,
    Boolean,
    UniqueConstraint,
    ForeignKey
)
from pydantic import BaseModel, ConfigDict, Field
from database import Base


# ==========================================
# SQLAlchemy ORM Models
# ==========================================

class Student(Base):
    __tablename__ = "students"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    student_id = Column(String(64), unique=True, index=True, nullable=False)
    name = Column(String(255), nullable=False)
    device_id = Column(String(255), index=True, nullable=False)
    enrollment_key = Column(String(128), nullable=False)
    enrolled_at = Column(BigInteger, default=lambda: int(time.time() * 1000), nullable=False)
    is_active = Column(Boolean, default=True, nullable=False)


class Session(Base):
    __tablename__ = "sessions"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    session_id = Column(String(64), unique=True, index=True, nullable=False)
    name = Column(String(255), nullable=False)
    teacher_name = Column(String(255), nullable=False)
    start_time = Column(BigInteger, default=lambda: int(time.time() * 1000), nullable=False)
    end_time = Column(BigInteger, nullable=True)
    duration_minutes = Column(Integer, default=15, nullable=False)
    status = Column(String(32), default="ACTIVE", nullable=False)
    rssi_threshold = Column(Integer, default=-75, nullable=False)
    current_challenge = Column(String(128), nullable=True)
    challenge_expires_at = Column(BigInteger, nullable=True)


class AttendanceRecord(Base):
    __tablename__ = "attendance_records"
    __table_args__ = (
        UniqueConstraint("student_id", "session_id", name="uq_student_session"),
    )

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    student_id = Column(String(64), index=True, nullable=False)
    session_id = Column(String(64), index=True, nullable=False)
    student_name = Column(String(255), nullable=False)
    check_in_time = Column(BigInteger, default=lambda: int(time.time() * 1000), nullable=False)
    rssi_median = Column(Integer, nullable=False)
    rssi_samples = Column(String(255), nullable=False)
    device_id = Column(String(255), index=True, nullable=True)
    status = Column(String(32), default="ACCEPTED", nullable=False)
    rejection_reason = Column(String(255), nullable=True)
    challenge_used = Column(String(128), nullable=False)
    synced_to_backend = Column(Boolean, default=True, nullable=False)


class AuditLog(Base):
    __tablename__ = "audit_log"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    timestamp = Column(BigInteger, default=lambda: int(time.time() * 1000), nullable=False)
    event_type = Column(String(64), index=True, nullable=False)
    session_id = Column(String(64), index=True, nullable=True)
    student_id = Column(String(64), nullable=True)
    details = Column(String(500), nullable=False)


# ==========================================
# Pydantic Schemas
# ==========================================

class StudentRegisterRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    student_id: str = Field(..., alias="studentId")
    name: str
    device_id: str = Field(..., alias="deviceId")
    enrollment_key: Optional[str] = Field(None, alias="enrollmentKey")


class StudentResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int
    student_id: str = Field(..., alias="studentId")
    name: str
    device_id: str = Field(..., alias="deviceId")
    enrollment_key: str = Field(..., alias="enrollmentKey")
    enrolled_at: int = Field(..., alias="enrolledAt")
    is_active: bool = Field(..., alias="isActive")


class SessionStartRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    session_id: Optional[str] = Field(None, alias="sessionId")
    name: str
    teacher_name: str = Field(..., alias="teacherName")
    duration_minutes: int = Field(15, alias="durationMinutes")
    rssi_threshold: int = Field(-75, alias="rssiThreshold")
    initial_challenge: Optional[str] = Field(None, alias="initialChallenge")


class SessionEndRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    end_time: Optional[int] = Field(None, alias="endTime")


class SessionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int
    session_id: str = Field(..., alias="sessionId")
    name: str
    teacher_name: str = Field(..., alias="teacherName")
    start_time: int = Field(..., alias="startTime")
    end_time: Optional[int] = Field(None, alias="endTime")
    duration_minutes: int = Field(..., alias="durationMinutes")
    status: str
    rssi_threshold: int = Field(..., alias="rssiThreshold")
    current_challenge: Optional[str] = Field(None, alias="currentChallenge")
    challenge_expires_at: Optional[int] = Field(None, alias="challengeExpiresAt")


class AttendanceSubmitRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    student_id: str = Field(..., alias="studentId")
    session_id: str = Field(..., alias="sessionId")
    student_name: Optional[str] = Field(None, alias="studentName")
    check_in_time: Optional[int] = Field(None, alias="checkInTime")
    rssi_median: int = Field(..., alias="rssiMedian")
    rssi_samples: str = Field(..., alias="rssiSamples")
    device_id: Optional[str] = Field(None, alias="deviceId")
    status: str = Field("ACCEPTED")
    rejection_reason: Optional[str] = Field(None, alias="rejectionReason")
    challenge_used: str = Field(..., alias="challengeUsed")
    hmac: Optional[str] = None
    timestamp: Optional[int] = None


class AttendanceResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int
    student_id: str = Field(..., alias="studentId")
    session_id: str = Field(..., alias="sessionId")
    student_name: str = Field(..., alias="studentName")
    check_in_time: int = Field(..., alias="checkInTime")
    rssi_median: int = Field(..., alias="rssiMedian")
    rssi_samples: str = Field(..., alias="rssiSamples")
    device_id: Optional[str] = Field(None, alias="deviceId")
    status: str
    rejection_reason: Optional[str] = Field(None, alias="rejectionReason")
    challenge_used: str = Field(..., alias="challengeUsed")
    synced_to_backend: bool = Field(..., alias="syncedToBackend")


class AuditLogResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: int
    timestamp: int
    event_type: str = Field(..., alias="eventType")
    session_id: Optional[str] = Field(None, alias="sessionId")
    student_id: Optional[str] = Field(None, alias="studentId")
    details: str


class AnalyticsSummaryResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True, from_attributes=True)

    total_sessions: int = Field(..., alias="totalSessions")
    active_sessions: int = Field(..., alias="activeSessions")
    total_students: int = Field(..., alias="totalStudents")
    total_attendance_records: int = Field(..., alias="totalAttendanceRecords")
    accepted_records: int = Field(..., alias="acceptedRecords")
    rejected_records: int = Field(..., alias="rejectedRecords")
    overall_attendance_rate: float = Field(..., alias="overallAttendanceRate")
