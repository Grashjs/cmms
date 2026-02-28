import WorkOrder from './workOrder';

export type RelationTypeInternal =
  | 'DUPLICATE_OF'
  | 'RELATED_TO'
  | 'SPLIT_FROM'
  | 'BLOCKS';
export type RelationType =
  | 'DUPLICATE_OF'
  | 'DUPLICATED_BY'
  | 'RELATED_TO'
  | 'SPLIT_TO'
  | 'SPLIT_FROM'
  | 'BLOCKED_BY'
  | 'BLOCKS';
export default interface Relation {
  id: number;
  parent: WorkOrder;
  child: WorkOrder;
  relationType: RelationTypeInternal;
}

export const relationTypes: RelationType[] = [
  'DUPLICATE_OF',
  'DUPLICATED_BY',
  'RELATED_TO',
  'SPLIT_TO',
  'SPLIT_FROM',
  'BLOCKED_BY',
  'BLOCKS'
];
