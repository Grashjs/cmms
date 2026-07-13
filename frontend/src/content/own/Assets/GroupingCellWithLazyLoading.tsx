import * as React from 'react';
import {
  DataGridProProps,
  getDataGridUtilityClass,
  GridRenderCellParams,
  useGridApiContext,
  useGridRootProps
} from '@mui/x-data-grid-pro';
import { unstable_composeClasses as composeClasses } from '@mui/material';
import Box from '@mui/material/Box';
import IconButton, { IconButtonProps } from '@mui/material/IconButton';

const isNavigationKey = (key: string) =>
  key === 'Home' ||
  key === 'End' ||
  key.indexOf('Arrow') === 0 ||
  key.indexOf('Page') === 0 ||
  key === ' ';
const useUtilityClasses = (ownerState: {
  classes: DataGridProProps['classes'];
}) => {
  const { classes } = ownerState;

  const slots = {
    root: ['treeDataGroupingCell'],
    toggle: ['treeDataGroupingCellToggle']
  };

  return composeClasses(slots, getDataGridUtilityClass, classes);
};
interface GroupingCellWithLazyLoadingProps extends GridRenderCellParams {}
export function GroupingCellWithLazyLoading(
  props: GroupingCellWithLazyLoadingProps
) {
  const { id, field, rowNode, row } = props;

  const rootProps = useGridRootProps();
  const apiRef = useGridApiContext();
  const classes = useUtilityClasses({ classes: rootProps.classes });

  const Icon = rowNode.childrenExpanded
    ? rootProps.components.TreeDataCollapseIcon
    : rootProps.components.TreeDataExpandIcon;

  const handleKeyDown: IconButtonProps['onKeyDown'] = (event) => {
    if (event.key === ' ') {
      event.stopPropagation();
    }
    if (isNavigationKey(event.key) && !event.shiftKey) {
      apiRef.current.publishEvent('cellNavigationKeyDown', props, event);
    }
  };

  const handleClick: IconButtonProps['onClick'] = (event) => {
    apiRef.current.setRowChildrenExpansion(id, !rowNode.childrenExpanded);
    apiRef.current.setCellFocus(id, field);
    event.stopPropagation();
  };

  return (
    <Box className={classes.root} sx={{ ml: rowNode.depth * 2 }}>
      <div className={classes.toggle}>
        {row.hasChildren && (
          <IconButton
            size="small"
            onClick={handleClick}
            onKeyDown={handleKeyDown}
            tabIndex={-1}
            aria-label={
              rowNode.childrenExpanded
                ? apiRef.current.getLocaleText('treeDataCollapse')
                : apiRef.current.getLocaleText('treeDataExpand')
            }
          >
            <Icon fontSize="inherit" />
          </IconButton>
        )}
      </div>
    </Box>
  );
}
