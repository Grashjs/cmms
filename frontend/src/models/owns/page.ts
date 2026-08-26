import type { Paths } from 'type-fest';

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
/**
 * Ein Feldpfad in einer Backend-Suchanfrage.
 *
 * `Paths<T>` aus type-fest schlaegt die Felder des Modells vor, kann aber die
 * Traversierung in eine Collection nicht ausdruecken: `"customFieldValues.value"`
 * wird abgelehnt, waehrend der SpecificationBuilder im Backend genau diesen Pfad
 * akzeptiert. Ausserdem liefert `Paths<T>` fuer Array-Indizes `number`, was nicht zu
 * `FilterField.field: string` passt.
 *
 * `string & {}` haelt die Vorschlaege aus `Paths<T>` in der Editor-Vervollstaendigung
 * sichtbar und laesst die tieferen Pfade trotzdem zu. Das ist ein Vorschlag, keine
 * Garantie - einen Tippfehler im Feldnamen faengt das Backend, nicht der Compiler.
 */
// eslint-disable-next-line @typescript-eslint/ban-types
export type QueryPath<T> = Extract<Paths<T>, string> | (string & {});

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
