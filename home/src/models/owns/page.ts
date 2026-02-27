export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
  sort: { empty: boolean; sorted: boolean; unsorted: boolean };
}
type JoinType = 'INNER' | 'LEFT' | 'RIGHT';
export type SearchOperator =
  | 'cn'
  | 'nc'
  | 'eq'
  | 'ne'
  | 'bw'
  | 'bn'
  | 'ew'
  | 'en'
  | 'nu'
  | 'nn'
  | 'gt'
  | 'ge'
  | 'lt'
  | 'le'
  | 'in'
  | 'inm';
type EnumName = 'STATUS' | 'PRIORITY' | 'JS_DATE';
export interface FilterField {
  field: string;
  joinType?: JoinType;
  value: any;
  operation: SearchOperator;
  enumName?: EnumName;
  values?: any[];
  alternatives?: FilterField[];
}
export type SortDirection = 'ASC' | 'DESC';
export interface SearchCriteria {
  filterFields: FilterField[];
  direction?: SortDirection;
  pageNum?: number;
  pageSize?: number;
  sortField?: string;
}
export const getInitialPage = <T>(): Page<T> => {
  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    last: true,
    size: 10,
    number: 0,
    numberOfElements: 0,
    first: true,
    empty: true,
    sort: { empty: true, sorted: true, unsorted: false }
  };
};

export type Sort = `${string},asc` | `${string},desc`;

export interface Pageable {
  page: number;
  size: number;
  sort?: Sort[];
}

export function pageableToQueryParams(pageable: Pageable): string {
  const params: string[] = [];

  params.push(`page=${pageable.page}`);
  params.push(`size=${pageable.size}`);

  if (pageable.sort) {
    for (const sortValue of pageable.sort) {
      params.push(`sort=${sortValue}`); // No encoding here, comma stays as is
    }
  }

  return params.join('&');
}
