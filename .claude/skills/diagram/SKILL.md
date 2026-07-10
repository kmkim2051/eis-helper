---
name: diagram
description: 유스케이스, 시퀀스, ERD, 상태, 플로우 등 다이어그램을 mermaid로 그려 docs/diagrams/ 아래 md 파일로 작성한다.
---

# 다이어그램 작성

요청받은 다이어그램을 mermaid로 그려 `docs/diagrams/<주제>.md` 파일로 저장한다.
파일명은 kebab-case 영문으로 (예: `usecase.md`, `scoring-sequence.md`, `domain-erd.md`).

## 문서 형식

다이어그램만 덜렁 넣지 않는다. 항상 아래 구조로 작성한다.

```markdown
# <다이어그램 제목> — <대상 요약>

## 개요
이 다이어그램이 무엇을 나타내고 왜 필요한지 2~3줄.

## 다이어그램
​```mermaid
...
​```

## 설명
등장 요소(액터/엔티티/상태 등)와 주요 흐름을 표 또는 목록으로.

## 참고
관련 문서 링크 (docs/product/ 등). 설계 근거가 있는 문서를 연결한다.
```

## 다이어그램 타입 선택

| 목적 | mermaid 문법 |
|---|---|
| 유스케이스 | 전용 문법 없음 — `flowchart LR`로 에뮬레이션: 액터는 `(( ))` 원형, 유스케이스는 `([ ])` 캡슐형, 시스템 경계는 `subgraph`, include/extend는 점선 `-. include .->` |
| 시퀀스 (API 호출, 채점 흐름) | `sequenceDiagram` |
| 도메인 모델 / 클래스 | `classDiagram` |
| ERD (테이블 관계) | `erDiagram` |
| 상태 전이 (채점 status 등) | `stateDiagram-v2` |
| 프로세스 / 분기 흐름 | `flowchart TD` |
| 일정 | `gantt` |

## 규칙

- 라벨은 한국어로, 코드/필드명은 그대로 영문으로 쓴다.
- 한글·특수문자(`/`, 괄호 등)가 들어간 라벨은 반드시 큰따옴표로 감싼다: `uc1(["채점 결과 조회"])`.
- 한 파일 = 한 주제. 같은 주제의 보조 다이어그램은 같은 파일에 섹션으로 추가한다.
- 설계가 바뀌면 새 파일을 만들지 말고 기존 파일의 다이어그램을 수정한다.
- 요소가 15개를 넘으면 다이어그램을 나누거나 상세를 생략한다. 다이어그램은 한눈에 읽혀야 한다.
- 그리기 전에 관련 문서(docs/product/ 등)를 확인해 용어와 모델명을 문서와 일치시킨다 (예: Problem, ScoringRubric, ScoringCriterion).
- 작성 후 mermaid 문법 오류가 없는지 검토한다 (닫히지 않은 subgraph, 따옴표 누락 등).
