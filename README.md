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
  - 동일 상위 카테고리의 하위에 카테고리명 중복 불가 

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
  - `parent_id`를 지정하지 않으면 전체 트리 반환
  - `parent_id`를 지정하면 해당 카테고리와 그 하위 카테고리를 포함한 트리 반환

#### 요청 정보
- Method: `GET`
- URL: `/api/categories`

#### Query Parameters
| 이름               | 타입      | 필수 | 설명                            | default |
|------------------|---------|----|-------------------------------|---------|
| parent_id        | Long    | X  | 조회 시작 카테고리 ID, NULL: 전체 트리 반환 | -       |

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