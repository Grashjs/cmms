import { MenuItem, TextField } from '@mui/material';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../../store';
import { getLocationsMini } from '../../../../../slices/location';
import { getUsersMini } from '../../../../../slices/user';
import { getTeamsMini } from '../../../../../slices/team';
import { getCategories } from '../../../../../slices/category';
import { ValueType } from '../../../../../models/owns/automation';

/**
 * Which list an `ENTITY_*` value type is picked from.
 *
 * <p>The server names the *kind* of reference a value is; where that kind is fetched from is a
 * frontend concern, so it lives here and nowhere else. An entity type missing from this map is
 * not a crash: the input degrades to a numeric id field, so a resolver added on the server is
 * usable the same day even before its picker exists.
 */
const CATEGORY_PATHS: { [entityType: string]: string } = {
  ASSET_CATEGORY: 'asset-categories',
  WORK_ORDER_CATEGORY: 'work-order-categories',
  PURCHASE_ORDER_CATEGORY: 'purchase-order-categories'
};

export interface Option {
  value: string;
  label: string;
}

/**
 * The options for every entity kind the editor can offer, with the fetches they need.
 *
 * <p>One hook rather than a fetch per input, because the same list is used by several rows and
 * React would otherwise dispatch it once per row.
 */
export function useEntityOptions(customFields: Option[]): {
  optionsFor: (entityType: string) => Option[] | null;
} {
  const dispatch = useDispatch();
  const { locationsMini } = useSelector((state) => state.locations);
  const { usersMini } = useSelector((state) => state.users);
  const { teamsMini } = useSelector((state) => state.teams);
  const { categories } = useSelector((state) => state.categories);

  useEffect(() => {
    if (!locationsMini.length) dispatch(getLocationsMini());
    if (!usersMini.length) dispatch(getUsersMini());
    if (!teamsMini.length) dispatch(getTeamsMini());
    Object.values(CATEGORY_PATHS).forEach((path) => {
      if (!categories[path]) dispatch(getCategories(path));
    });
  }, []);

  const optionsFor = (entityType: string): Option[] | null => {
    if (CATEGORY_PATHS[entityType]) {
      return (categories[CATEGORY_PATHS[entityType]] ?? []).map((category) => ({
        value: String(category.id),
        label: category.name
      }));
    }
    switch (entityType) {
      case 'LOCATION':
        return locationsMini.map((location) => ({
          value: String(location.id),
          label: location.name
        }));
      case 'USER':
        return usersMini.map((user) => ({
          value: String(user.id),
          label: `${user.firstName} ${user.lastName}`
        }));
      case 'TEAM':
        return teamsMini.map((team) => ({
          value: String(team.id),
          label: team.name
        }));
      case 'CUSTOM_FIELD':
        // Not fetched: the metadata endpoint already carries this company's asset custom
        // fields, complete with their options, which is what lets the value input below offer
        // a choice field's actual options instead of a free-text box.
        return customFields;
      default:
        return null;
    }
  };

  return { optionsFor };
}

interface ValueInputProps {
  valueType: ValueType;
  options: string[];
  value: string;
  onChange: (value: string) => void;
  label?: string;
  /** Rendered under the field; used for the placeholder hint and category bindings. */
  helperText?: string;
  optionsFor: (entityType: string) => Option[] | null;
  disabled?: boolean;
  required?: boolean;
}

/**
 * One input, chosen from the server's description of the value rather than from a table in the
 * frontend. This function is the entire mapping from metadata to UI — and it is the reason the
 * editor needs no changes when a resolver or handler is added: a new subject arrives with a
 * value type this already knows how to render.
 */
export default function ValueInput({
  valueType,
  options,
  value,
  onChange,
  label,
  helperText,
  optionsFor,
  disabled,
  required
}: ValueInputProps) {
  const { t }: { t: any } = useTranslation();

  const entityOptions = valueType.startsWith('ENTITY_')
    ? optionsFor(valueType.substring('ENTITY_'.length))
    : null;

  // A fixed reference to the triggering entity. Rendered as a select of exactly the permitted
  // placeholders, so "this asset" cannot be mistyped as a literal id pointing at one machine.
  if (valueType === 'TRIGGER_REFERENCE') {
    return (
      <TextField
        select
        fullWidth
        size="small"
        label={label}
        value={value}
        required={required}
        disabled={disabled}
        helperText={helperText}
        onChange={(event) => onChange(event.target.value)}
      >
        <MenuItem value="">{t('automation_none')}</MenuItem>
        {options.map((option) => (
          <MenuItem key={option} value={option}>
            {t('automation_triggering_entity')}
          </MenuItem>
        ))}
      </TextField>
    );
  }

  if ((valueType === 'ENUM' || valueType === 'CHOICE') && options.length) {
    return (
      <TextField
        select
        fullWidth
        size="small"
        label={label}
        value={value}
        required={required}
        disabled={disabled}
        helperText={helperText}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map((option) => (
          <MenuItem key={option} value={option}>
            {/* An ENUM is application vocabulary and translated if a key exists; a CHOICE is
                the user's own option text and shown verbatim. */}
            {valueType === 'ENUM' ? t(option) : option}
          </MenuItem>
        ))}
      </TextField>
    );
  }

  if (entityOptions) {
    return (
      <TextField
        select
        fullWidth
        size="small"
        label={label}
        value={value}
        required={required}
        disabled={disabled}
        helperText={helperText}
        onChange={(event) => onChange(event.target.value)}
      >
        {entityOptions.length === 0 && (
          <MenuItem value="" disabled>
            {t('automation_nothing_to_choose')}
          </MenuItem>
        )}
        {entityOptions.map((option) => (
          <MenuItem key={option.value} value={option.value}>
            {option.label}
          </MenuItem>
        ))}
      </TextField>
    );
  }

  return (
    <TextField
      fullWidth
      size="small"
      label={label}
      value={value}
      required={required}
      disabled={disabled}
      helperText={helperText}
      type={valueType === 'NUMBER' ? 'number' : 'text'}
      onChange={(event) => onChange(event.target.value)}
    />
  );
}
