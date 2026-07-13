import * as React from 'react';
import {
  gridFilteredDescendantCountLookupSelector,
  GridRenderCellParams,
  useGridApiContext,
  useGridSelector
} from '@mui/x-data-grid-pro';
import Box from '@mui/material/Box';
import Button, { ButtonProps } from '@mui/material/Button';
import { useTranslation } from 'react-i18next';

export const isNavigationKey = (key: string) =>
  key === 'Home' ||
  key === 'End' ||
  key.indexOf('Arrow') === 0 ||
  key.indexOf('Page') === 0 ||
  key === ' ';

export default function CustomGridTreeDataGroupingCell(
  props: GridRenderCellParams
) {
  const { t }: { t: any } = useTranslation();
  const { id, field, rowNode } = props;
  const apiRef = useGridApiContext();
  const filteredDescendantCountLookup = useGridSelector(
    apiRef,
    gridFilteredDescendantCountLookupSelector
  );
  const filteredDescendantCount =
    filteredDescendantCountLookup[rowNode.id] ?? 0;

  const handleKeyDown: ButtonProps['onKeyDown'] = (event) => {
    if (event.key === ' ') {
      event.stopPropagation();
    }
    if (isNavigationKey(event.key) && !event.shiftKey) {
      apiRef.current.publishEvent('cellNavigationKeyDown', props, event);
    }
  };

  const handleClick: ButtonProps['onClick'] = (event) => {
    apiRef.current.setRowChildrenExpansion(id, !rowNode.childrenExpanded);
    apiRef.current.setCellFocus(id, field);
    event.stopPropagation();
  };

  return (
    <Box sx={{ ml: rowNode.depth * 4 }}>
      <div>
        {filteredDescendantCount > 0 ? (
          <Button
            onClick={handleClick}
            onKeyDown={handleKeyDown}
            tabIndex={-1}
            size="small"
          >
            {t('show')} {filteredDescendantCount}{' '}
            {filteredDescendantCount === 1 ? 'Asset' : 'Assets'}
          </Button>
        ) : (
          <span />
        )}
      </div>
    </Box>
  );
}
