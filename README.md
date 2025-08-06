# 카테고리 API 개발 프로젝트(과제)

## 요구사항
- 목적: 온라인 쇼핑몰 상품 카테고리 구현
- 기능 구현 내용:
    - 카테고리 등록/수정/삭제 API
    - 카테고리 조회 API
        - 카테고리 조회 시 자기 자신 포함
        - 카테고리 지정 하지 않는 경우, 전체 카테고리 반환
        - 반환 구조: 트리
- 문서화 필수 내용:
    - 어플리케이션 실행 설치 및 빌드 방법
    - DB 명세
    - API 명세
- 필수 포함: RDBMS, REST API
- 기술 스택: Java & Spring

---

# 🛒 온라인 쇼핑몰 상품 카테고리
## 프로젝트 개요
- 온라인 쇼핑몰 상품 카테고리 등록/수정/삭제/조회 REST API 프로젝트
- 트리 구조 카테고리 관리

## 설계 시 고려사항
- 조회 성능 최적화
  - 상위, 하위 카테고리 순서를 경로 형태로 저장해서 재귀 호출 없이 단일 쿼리로 트리 조회
    - 전체 트리 및 특정 카테고리 기준으로 단일 쿼리로 조회 후 애플리케이션 로직에서 트리 형태 구성
  - path 컬럼 기반으로 빠르게 필터링 및 조회
- 운영 중 발생 가능한 이슈에 대한 유효성 검증
  - 등록 및 수정 시 동일 부모 카테고리 하위에 중복 이름 등록 방지
  - 수정 시 자기자신 또는 하위 카테고리를 부모로 설정하는 순환 참조 방지
  - 삭제 시 하위 카테고리 존재하는 경우 삭제 불가 처리
- 확장성과 유지보수 고려
  - Soft Delete 적용으로 데이서 삭제 히스토리 유지 및 롤백 고려
  - 엔티티 중심 설계로 변경 및 확장 시 유연성 확보

## 추후 개선 사항
- 조회 성능 향상을 위한 캐싱 기능 추가
  - Redis 등으로 전체 트리 구조 캐싱(응답 속도 개선)
  - 카테고리는 자주 변경되지 않기 때문에 캐싱 시 성능 향상 이점 있음
- 어드민 전용 권한 체크
  - 카테고리 등록/수정/삭제는 어드민 권한만 접근 가능하도록 인증 적용
- 어드민 전용 조회 옵션 추가
  - 미노출 상태까지 모두 노출 되는 관리자 전용 조회 API

---
## 기술 스택
| 구분        | 기술                |
|-----------|-------------------|
| Language  | Java 21           |
| Framework | Spring Boot 3.5.4 |
| DB        | H2                |
| docs      | Swagger           |

---

## 프로젝트 구조
```text
shop/
├── build.gradle
├── settings.gradle
├── gradlew, gradlew.bat
├── README.md
├── HELP.md
├── src/
│   ├── main/
│   │   ├── java/com/musinsa/shop/
│   │   │   ├── ShopApplication.java
│   │   │   ├── common/                        # 공통 설정 및 예외 처리
│   │   │   │   ├── advice/                    # 글로벌 예외 핸들러
│   │   │   │   ├── config/                    # JPA, Querydsl, Swagger 설정
│   │   │   │   ├── exception/                 # 커스텀 예외 클래스
│   │   │   │   └── response/                  # 공통 응답 포맷 클래스
│   │   │   └── domain/category/               # 카테고리 도메인
│   │   │       ├── controller/                # API 컨트롤러
│   │   │       ├── dto/                       # 요청/응답 DTO
│   │   │       ├── entity/                    # JPA 엔티티
│   │   │       ├── repository/                # JPA + Querydsl Repository
│   │   │       └── service/                   # 비즈니스 로직 서비스
│   │   └── resources/
│   │       ├── application.yml                # 환경설정 파일
│   │       ├── static/
│   │       └── templates/
│   └── test/
│       ├── java/com/musinsa/shop/category/
│       │   ├── integration/                   # 통합 테스트
│       │   └── service/                       # 서비스 단위 테스트
│       └── resources/
│           └── application-test.yml           # 테스트용 설정 파일
```
---

## DB 명세
### 테이블 : category
| 필드명           | 타입           | 설명                                    | 제약조건               |
|---------------|--------------|---------------------------------------|--------------------|
| id            | BIGINT       | 카테고리 ID                               | PK, Auto Increment |
| title         | VARCHAR(50)  | 카테고리 이름                               | NOT NULL           |
| parent_id     | BIGINT       | 상위 카테고리 ID (루트: NULL)                 | 자기참조 필드, nullable  |
| path          | VARCHAR(512) | 전체 경로 (카테고리 ID 나열, 구분자 사용)	           | nullable           |
| display_order | INT          | 정렬 순서 (동일 parent 내)                   | default 9999       |
| link          | VARCHAR(512) | 클릭 이동 URL                             | nullable           |
| active        | BOOLEAN      | 카테고리 표시 여부 (true: 노출, false: 숨김)      | default true       |
| deleted       | BOOLEAN      | Soft Delete 여부 (false: 미삭제, true: 삭제) | default false      |
| deleted_at    | DATETIME     | 삭제 일시                                 | nullable           |
| created_at    | DATETIME     | 생성 일시                                 | NOT NULL           |
| updated_at    | DATETIME     | 마지막 수정 일시                             | NOT NULL           |


---
## API 문서
### 엔드포인트 목록
| API 이름 | 메서드    | URI                  |
|--------|--------|----------------------|
| 카테고리 등록 | POST   | `/api/categories`     |
| 카테고리 수정 | PATCH  | `/api/categories/{id}` |
| 카테고리 삭제 | DELETE | `/api/categories/{id}` | 
| 카테고리 조회 | GET    | `/api/categories`     |

### 공통 응답 형식
#### 성공 응답 예시
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "title": "상의"
  }
}
```

#### 실패 응답 예시
```json
{
  "code": "INVALID_REQUEST",
  "message": "유효하지 않은 요청입니다.",
  "data": {
    "field": "title"
  }
}
```

### 공통 에러 코드
| HTTP 상태 | 에러 코드                | 메시지               | 설명                       |
|-----------|----------------------|-------------------|--------------------------|
| 400       | `INVALID_REQUEST`    | 유효하지 않은 요청입니다.    | 필드 유효성 실패 등 잘못된 클라이언트 요청 |
| 404       | `RESOURCE_NOT_FOUND` | 리소스를 찾을 수 없습니다.   | 존재하지 않는 데이터 요청 등         |
| 409       | `DUPLICATE_RESOURCE` | 중복된 요청입니다.        | 이미 존재하는 리소스를 등록하려고 할 때   |
| 500       | `SERVER_ERROR`       | 서버 내부 오류입니다.      | 알 수 없는 시스템 오류 발생 시       |

### 1. 카테고리 등록
- 신규 카테고리를 등록합니다.
  - 유효성: 동일 상위 카테고리의 하위에 카테고리명 중복 불가 

#### 요청 정보
- Method: `POST`
- URL: `/api/categories`
- Content-Type: `application/json`

#### Request Body
| 필드명          | 타입      | 필수 | 설명                         | default |
|--------------|---------|----|----------------------------|---------|
| title        | String  | O  | 카테고리 이름                    | -       |
| parentId     | Long    | X  | 상위 카테고리 ID (루트는 null)      | -       |
| displayOrder | Integer | X  | 정렬 순서                      | 9999    |
| link         | String  | X  | 클릭 이동 URL                  | -       |
| active       | Boolean | X  | 카테고리 표시 여부 (default: true) | true    |

##### Request Body 예시
```json
{
  "title": "상의",
  "parentId": null,
  "displayOrder": 0,
  "link": "/category/top",
  "active": true
}
```

#### 응답 예시
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "title": "상의"
  }
}
```

### 2. 카테고리 수정
- 기존 카테고리 수정합니다.
  - 유효성: 
    - 자기 자신을 부모로 지정 불가
    - 본인의 하위 카테고리를 부모로 지정 불가
    - 동일한 부모 카테고리 내 하위 이름은 중복 될 수 없음

#### 요청 정보
- Method: `PATCH`
- URL: `/api/categories/{id}`
- Content-Type: `application/json`

#### Path
| 이름 | 타입   | 필수 | 설명          |
|------|------|------|-------------|
| id   | Long | O    | 수정할 카테고리 ID |

#### Request Body
| 필드명          | 타입      | 필수 | 설명                   | default |
|--------------|---------|----|----------------------|---------|
| title        | String  | X  | 카테고리 이름              | -       |
| parentId     | Long    | X  | 상위 카테고리 ID (루트는 null) | -       |
| displayOrder | Integer | X  | 정렬 순서                | -       |
| link         | String  | X  | 클릭 이동 URL            | -       |
| active       | Boolean | X  | 카테고리 표시 여부           | -       |

##### Request Body 예시
```json
{
  "title": "상의-NEW",
  "parentId": null,
  "displayOrder": 1,
  "link": "/category/top-new",
  "active": true
}
```

#### 응답 예시
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "title": "상의-NEW"
  }
}
```

### 3. 카테고리 삭제
- 기존 카테고리를 삭제합니다.
    - Soft Delete: 실제 데이터 삭제가 아닌 flag 값으로 삭제 처리
    - 유효성: 하위 카테고리가 없는 경우만 삭제 가능

#### 요청 정보
- Method: `DELETE`
- URL: `/api/categories/{id}`

#### Path
| 이름 | 타입 | 필수 | 설명      |
|------|------|------|---------|
| id   | Long | O    | 카테고리 ID |

#### 응답 예시
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": null
}
```

### 4. 카테고리 조회
- 카테고리를 트리 구조로 조회합니다.
  - parentId 지정하지 않으면 전체 트리 반환
  - parentId 지정하면 해당 카테고리와 그 하위 카테고리를 포함한 트리 반환

#### 요청 정보
- Method: `GET`
- URL: `/api/categories`

#### Query Parameters
| 이름               | 타입      | 필수 | 설명                            | default |
|------------------|---------|----|-------------------------------|---------|
| parentId        | Long    | X  | 조회 시작 카테고리 ID, NULL: 전체 트리 반환 | -       |

#### 응답 예시
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": [
    {
      "id": 1,
      "title": "패션",
      "parentId": null,
      "link": "/category/fashion",
      "displayOrder": 1,
      "active": true,
      "children": [
        {
          "id": 2,
          "title": "남성의류",
          "parentId": 1,
          "link": "/category/male",
          "displayOrder": 1,
          "active": true,
          "children": []
        },
        {
          "id": 3,
          "title": "여성의류",
          "parentId": 1,
          "link": "/category/female",
          "displayOrder": 2,
          "active": false,
          "children": []
        }
      ]
    }
  ]
}
```

---
## 로컬 실행 가이드
1. git clone
```bash
git clone https://github.com/joyj10/test-category-be.git
cd test-category-be
```

2. 서버 실행
```bash
./gradlew bootRun
```

3. 문서 및 테스트 UI 접근(스웨거)
- Swagger
```text
http://localhost:8080/api/swagger-ui/index.html
```

---
## 통합 테스트 시나리오
| 구분 (성공/실패) | 시나리오 설명                      | 목적 및 검증 포인트                                    |
|------------|------------------------------|------------------------------------------------|
| 등록 (성공)    | 루트 카테고리 등록                   | `parentId = null` 등록 시 ID, title 응답 확인         |
| 등록 (실패)    | 부모 없는 하위 카테고리 등록             | 존재하지 않는 parentId로 등록 요청 시 `404 Not Found` 발생   |
| 등록 (실패)    | 동일 부모 내 중복 이름 등록             | 같은 parentId 하위에 title 중복 시 `409 Conflict` 발생   |
| 등록 (실패)    | 이름 공백 등록                     | title 빈값 입력 시 `400 Bad Request` 발생             |
| 수정 (성공)    | 부모 없는 카테고리 필드 수정             | title, link, displayOrder 수정 및 DB 반영 확인        |
| 수정 (성공)    | 부모 변경 시 본인 및 하위 카테고리 path 일괄 변경 | 부모 변경 시 path가 본인/하위까지 변경되는지 확인                 |
| 수정 (실패)    | 존재하지 않는 카테고리 ID 수정           | 잘못된 ID로 수정 요청 시 `404 Not Found` 발생             |
| 수정 (실패)    | 존재하지 않는 부모 ID 지정             | 없는 부모 ID로 변경 요청 시 `404 Not Found` 발생           |
| 수정 (실패)    | 자기 자신을 부모로 지정                | `parentId = selfId` 입력 시 `400 Bad Request` 발생  |
| 수정 (실패)    | 하위를 부모로 지정 (순환 참조)           | 본인 하위 카테고리로 변경 요청 시 `400 Bad Request` 발생       |
| 수정 (실패)    | 변경된 부모에 동일한 title 존재         | 새로운 부모 아래 동일한 이름 존재 시 `409 Conflict` 발생        |
| 삭제 (성공)    | 하위 없는 카테고리 삭제                | `deleted=true`, `deletedAt != null` 확인         |
| 삭제 (실패)    | 하위 존재 시 삭제 시도                | 삭제 요청 카테고리에 하위 카테고리 존재 하면 `400 Bad Request` 발생 |
| 조회 (성공)    | 전체 트리 조회                     | 루트부터 모든 트리 구조 리턴 (하위 카테고리 포함)                  |
| 조회 (성공)    | 특정 parentId 기준 트리 조회         | parentId 기준으로 시작하는 트리 구조 리턴                    |
| 조회 (실패)    | 존재하지 않는 parentId 조회          | 잘못된 ID로 조회 시 `404 Not Found` 발생                |
