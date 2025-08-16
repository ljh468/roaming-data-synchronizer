# Roaming Data Synchronizer

## 📋 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [시스템 요구사항](#시스템-요구사항)
- [시작하기](#시작하기)
- [실행 방법](#실행-방법)
- [배치 Job 가이드](#배치-job-가이드)
- [테스트 시나리오](#테스트-시나리오)
- [모니터링 및 트러블슈팅](#모니터링-및-트러블슈팅)

## 프로젝트 소개
이 프로젝트는 블로그 글 ['데이터 동기화 스케줄러, 나는 왜 회사에서 Spring Batch를 선택했을까?'](https://jh2021.tistory.com/44)에서 다룬 문제들을 코드로 해결하는 실습 예제입니다.

단순 스케줄러가 가진 성능, 실패 복구, 모니터링의 한계를 Spring Batch가 어떻게 극복하는지 직접 실행해보며 이해하는 것을 목표로 합니다.


- **Chunk 기반 처리**: 대용량 데이터를 안전하고 효율적으로 처리하는 기본기를 다룹니다.
- **실패 복구성**: Skip, Retry 정책을 통해 일부 데이터 오류나 일시적 장애에 대응하는 방법을 학습합니다.
- **성능 최적화**: Partitioning을 이용한 병렬 처리로 배치 작업의 속도를 향상시킵니다.
- **Tasklet 활용**: 데이터 처리 전후의 정리 작업(파일 백업 등)을 Tasklet으로 구현해봅니다.

## 기술 스택
- Java: 21
- Spring Boot: 3.3.1
- Spring Batch: 5.x
- Spring Data JPA & PostgreSQL
- Docker, Gradle, Lombok

## 시스템 요구사항

- **Java**: 21 이상
- **Docker**: 데이터베이스 환경 구성
- **메모리**: 최소 2GB RAM
- **디스크**: 최소 1GB 여유 공간

## 시작하기

### 1. 소스코드 클론

```bash
git clone https://github.com/ljh468/roaming-data-synchronizer.git
cd roaming-data-synchronizer
```

### 2. 데이터베이스 실행 (Docker)

```bash
# PostgreSQL 컨테이너 실행
docker-compose up -d

# 데이터베이스 상태 확인
docker-compose ps
```

### 3. 프로젝트 빌드

```bash
# 의존성 다운로드 및 빌드
./gradlew clean build

# 테스트 실행
./gradlew test
```

## 실행 방법

### 기본 실행

```bash
# 전체 애플리케이션 실행 (모든 Job 실행 안됨)
./gradlew bootRun
```

### Job 실행 방법
```bash
# 1. 기본 청크 처리 Job
./gradlew bootRun --args="--spring.batch.job.name=chunkSyncJob"

# 2. 견고한 처리 Job (예외 처리 포함)
./gradlew bootRun --args="--spring.batch.job.name=robustSyncJob"

# 3. 파티셔닝 Job (병렬 처리)
./gradlew bootRun --args="--spring.batch.job.name=partitioningSyncJob"

# 4. 전체 워크플로우 Job (파일 백업 → 동기화 → 알림)
./gradlew bootRun --args="--spring.batch.job.name=fullSyncJob"
```

### Job 매개변수 사용

```bash
# 특정 파일로 처리
./gradlew bootRun --args="--spring.batch.job.name=chunkSyncJob --inputFile=custom-data.csv"

# 청크 크기 조정
./gradlew bootRun --args="--spring.batch.job.name=chunkSyncJob --chunkSize=20"
```

## 배치 Job 상세 설명

각 Job은 특정 학습 목표를 가지며, 블로그에서 다룬 개념들을 단계적으로 경험할 수 있도록 설계되었습니다.

### 1. chunkSyncJob
- **학습 목표**: Spring Batch의 가장 기본이 되는 Chunk 처리 방식 이해하기
- **주요 내용**:
    - `Reader` -> `Processor` -> `Writer`의 기본 구조 학습
    - 10개 단위(Chunk)로 데이터를 묶어 트랜잭션 처리
    - 대용량 데이터 처리의 기본 모델 파악

### 2. robustSyncJob
- **학습 목표**: "실패 복구 설계"의 중요성을 코드로 경험하기
- **주요 내용**:
    - `Skip` 정책: 특정 예외(`IllegalArgumentException`) 발생 시, 해당 데이터를 건너뛰고 계속 진행
    - `Retry` 정책: 일시적인 DB 장애(`TransientDataAccessException`) 발생 시, 최대 3회 재시도
    - `Listener` 활용: Job/Step 실행 전후의 로그를 기록하여 처리 과정 추적

### 3. partitioningSyncJob
- **학습 목표**: 병렬 처리를 통해 "성능상의 이점" 극대화하기
- **주요 내용**:
    - `Partitioner`를 사용하여 단일 파일을 4개의 처리 단위로 분할
    - `ThreadPoolTaskExecutor`를 이용해 각 단위를 별도의 스레드에서 병렬 처리
    - `chunkSyncJob`과 실행 시간을 비교하여 성능 향상 체감

### 4. fullSyncJob
- **학습 목표**: `Tasklet`과 `Chunk` Step을 조합하여 실무적인 워크플로우 구성하기
- **주요 내용**:
    - **1단계 (Tasklet)**: 데이터 처리 전, 기존 파일을 백업 디렉터리로 이동
    - **2단계 (Partitioning Step)**: 병렬 처리로 메인 데이터 동기화 작업 수행
    - **3단계 (Tasklet)**: 작업 완료 후, 실행 결과 요약 및 알림

## 테스트 시나리오

### 정상 처리 테스트

```bash
# 1. 기본 기능 테스트
./gradlew bootRun --args="--spring.batch.job.name=chunkSyncJob"

# 2. 예외 처리 테스트 (일부 데이터에 오류 포함)
./gradlew bootRun --args="--spring.batch.job.name=robustSyncJob"

# 3. 성능 테스트
./gradlew bootRun --args="--spring.batch.job.name=partitioningSyncJob"

# 4. 전체 워크플로우 테스트
./gradlew bootRun --args="--spring.batch.job.name=fullSyncJob"
```

### 데이터 검증

```bash
# PostgreSQL 접속
docker exec -it roaming-postgres psql -U roaming_user -d roaming_db

# 처리된 데이터 확인
SELECT COUNT(*) FROM roaming_status;
SELECT * FROM roaming_status LIMIT 10;
```

### 성능 측정

```bash
# 실행 시간 측정
time ./gradlew bootRun --args="--spring.batch.job.name=partitioningSyncJob"

# 메모리 사용량 모니터링
docker stats roaming-postgres
```

---

## 모니터링 및 트러블슈팅

### 로그 확인

```bash
# 애플리케이션 로그 (실행 중)
tail -f logs/application.log

# 배치 실행 결과 확인
grep "Job: \[SimpleJob" logs/application.log
```

### 일반적인 문제 해결

#### 1. 데이터베이스 연결 오류
```bash
# PostgreSQL 컨테이너 상태 확인
docker-compose ps

# 컨테이너 재시작
docker-compose restart postgres

# 로그 확인
docker logs roaming-postgres
```

#### 2. 메모리 부족 오류
```bash
# JVM 힙 메모리 증가
export JAVA_OPTS="-Xmx2g"
./gradlew bootRun --args="--spring.batch.job.name=partitioningSyncJob"
```

#### 3. 파일을 찾을 수 없음
```bash
# 데이터 파일 위치 확인
ls -la src/main/resources/data/

# 백업 디렉터리 확인
ls -la backup/
```

### Spring Batch 메타데이터 확인

```sql
-- Job 실행 이력
SELECT * FROM batch_job_instance ORDER BY job_instance_id DESC LIMIT 10;

-- Step 실행 상세
SELECT * FROM batch_step_execution ORDER BY step_execution_id DESC LIMIT 10;

-- 실패한 Job 확인
SELECT * FROM batch_job_execution WHERE status = 'FAILED';
```

## 🏗 프로젝트 구조

```
src/
├── main/
│   ├── java/com/roaming/
│   │   ├── config/
│   │   │   └── BatchConfig.java          # 배치 설정
│   │   ├── domain/
│   │   │   ├── RoamingData.java          # 입력 데이터 모델
│   │   │   └── RoamingStatusEntity.java  # 출력 엔티티
│   │   ├── job/
│   │   │   ├── listener/                 # 배치 리스너들
│   │   │   ├── partitioner/              # 파티셔너
│   │   │   ├── processor/                # 데이터 프로세서
│   │   │   └── tasklet/                  # Tasklet 구현체들
│   │   └── RoamingDataSynchronizerApplication.java
│   └── resources/
│       ├── data/
│       │   └── roaming-data-sample.csv   # 샘플 데이터
│       └── application.yml               # 애플리케이션 설정
├── test/                                 # 테스트 코드
└── backup/                               # 백업 디렉터리
```

## 문의

프로젝트에 대한 문의사항이나 개선 제안이 있으시면 이슈를 등록해 주세요.