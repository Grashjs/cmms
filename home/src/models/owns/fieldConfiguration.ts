export enum FieldType {
  OPTIONAL = 'OPTIONAL',
  HIDDEN = 'HIDDEN',
  REQUIRED = 'REQUIRED'
}
export interface FieldConfiguration {
  id: number;
  fieldName: string;
  fieldType: FieldType;
}
