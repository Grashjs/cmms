import {
  cloneElement,
  ReactElement,
  useContext,
  useEffect,
  useRef,
  useState
} from 'react';

import {
  alpha,
  Badge,
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Popover,
  styled,
  Tooltip,
  Typography,
  useTheme
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import Scrollbar from 'src/components/Scrollbar';
import NotificationsNoneTwoToneIcon from '@mui/icons-material/NotificationsNoneTwoTone';
import Text from 'src/components/Text';

import NotificationsActiveTwoToneIcon from '@mui/icons-material/NotificationsActiveTwoTone';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../../store';
import {
  editNotification,
  getMoreNotifications,
  getNotifications,
  newReceivedNotification,
  readAllNotifications
} from '../../../../../slices/notification';
import Notification, {
  NotificationType
} from '../../../../../models/owns/notification';
import {
  getAssetUrl,
  getLocationUrl,
  getMeterUrl,
  getPartUrl,
  getPurchaseOrderUrl,
  getRequestUrl,
  getTeamUrl,
  getWorkOrderUrl
} from '../../../../../utils/urlPaths';
import { CompanySettingsContext } from '../../../../../contexts/CompanySettingsContext';
import AssignmentTwoToneIcon from '@mui/icons-material/AssignmentTwoTone';
import MoveToInboxTwoToneIcon from '@mui/icons-material/MoveToInboxTwoTone';
import Inventory2TwoToneIcon from '@mui/icons-material/Inventory2TwoTone';
import LocationOnTwoToneIcon from '@mui/icons-material/LocationOnTwoTone';
import HandymanTwoToneIcon from '@mui/icons-material/HandymanTwoTone';
import SpeedTwoToneIcon from '@mui/icons-material/SpeedTwoTone';
import { People } from '@mui/icons-material';
import ReceiptTwoToneIcon from '@mui/icons-material/ReceiptTwoTone';
import { SearchCriteria } from '../../../../../models/owns/page';
import SockJS from 'sockjs-client';
import { apiUrl } from 'src/config';
import useAuth from 'src/hooks/useAuth';
import { Stomp } from '@stomp/stompjs';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';

const BoxComposed = styled(Box)(
  () => `
  position: relative;
`
);

const BoxComposedContent = styled(Box)(
  ({ theme }) => `
  position: relative;
  z-index: 7;

  .MuiTypography-root {
      color: ${theme.palette.primary.contrastText};

      & + .MuiTypography-root {
          color: ${alpha(theme.palette.primary.contrastText, 0.7)};
      }
  }
  
`
);

const BoxComposedImage = styled(Box)(
  () => `
  position: absolute;
  left: 0;
  top: 0;
  z-index: 5;
  filter: grayscale(80%);
  background-size: cover;
  height: 100%;
  width: 100%;
  border-radius: inherit;
`
);

const BoxComposedBg = styled(Box)(
  () => `
  position: absolute;
  left: 0;
  top: 0;
  z-index: 6;
  height: 100%;
  width: 100%;
  border-radius: inherit;
`
);

const IconButtonWrapper = styled(IconButton)(
  ({ theme }) => `
  width: ${theme.spacing(4)};
  height: ${theme.spacing(4)};
  border-radius: ${theme.general.borderRadiusLg};
`
);

function HeaderNotifications() {
  const ref = useRef<any>(null);
  const [isOpen, setOpen] = useState<boolean>(false);
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { notifications, currentPageNum, lastPage, loadingGet } = useSelector(
    (state) => state.notifications
  );
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const initialCriteria: SearchCriteria = {
    filterFields: [],
    pageSize: 25,
    pageNum: 0,
    direction: 'DESC'
  };
  const [criteria, setCriteria] = useState<SearchCriteria>(initialCriteria);
  const [stompClient, setStompClient] = useState(null);
  const { user } = useAuth();
  useEffect(() => {
    dispatch(getNotifications(criteria));
  }, []);
  useEffect(() => {
    const disconnect = () => {
      if (stompClient) {
        stompClient.disconnect();
        setStompClient(null);
      }
    };
    if (user) {
      if (!stompClient) {
        const socket = new SockJS(`${apiUrl}ws`);
        const client = Stomp.over(socket);
        client.connect(
          { token: localStorage.getItem('accessToken') },
          function (frame) {
            const subscription = client.subscribe(
              `/user/${user.email}/notifications`,
              function (message) {
                const notification: Notification = JSON.parse(message.body);
                dispatch(newReceivedNotification(notification));
              }
            );
            setStompClient(client);
          }
        );
      }
    } else {
      disconnect();
    }
    return disconnect;
  }, [user?.id, stompClient]);
  const handleOpen = (): void => {
    setOpen(true);
  };

  const handleClose = (): void => {
    setOpen(false);
  };
  const onReadNotification = (notification: Notification) => {
    let url;
    const id = notification.resourceId;
    switch (notification.notificationType) {
      case 'INFO':
        break;
      case 'ASSET':
        url = getAssetUrl(id);
        break;
      case 'REQUEST':
        url = getRequestUrl(id);
        break;
      case 'WORK_ORDER':
        url = getWorkOrderUrl(id);
        break;
      case 'PART':
        url = getPartUrl(id);
        break;
      case 'METER':
        url = getMeterUrl(id);
        break;
      case 'LOCATION':
        url = getLocationUrl(id);
        break;
      case 'TEAM':
        url = getTeamUrl(id);
        break;
      case 'PURCHASE_ORDER':
        url = getPurchaseOrderUrl(id);
        break;
      default:
        break;
    }
    if (notification.seen) {
      if (url) {
        navigate(url);
        handleClose();
      }
    } else
      dispatch(editNotification(notification.id, { seen: true }))
        .then(() => {
          if (url) {
            navigate(url);
          }
        })
        .finally(handleClose);
  };
  const notificationIcons: Record<NotificationType, ReactElement> = {
    ASSET: <HandymanTwoToneIcon />,
    LOCATION: <LocationOnTwoToneIcon />,
    METER: <SpeedTwoToneIcon />,
    PART: <Inventory2TwoToneIcon />,
    REQUEST: <MoveToInboxTwoToneIcon />,
    TEAM: <People />,
    WORK_ORDER: <AssignmentTwoToneIcon />,
    INFO: <NotificationsNoneTwoToneIcon />,
    PURCHASE_ORDER: <ReceiptTwoToneIcon />
  };
  // TODO It is not triggered
  const onScroll = (e) => {
    const bottom =
      e.target.scrollHeight - e.target.scrollTop === e.target.clientHeight;
    if (bottom && !loadingGet && !lastPage) {
      dispatch(getMoreNotifications(criteria, currentPageNum + 1));
    }
  };
  return (
    <>
      <Tooltip arrow title={t('Notifications')}>
        <Badge
          variant="dot"
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'right'
          }}
          sx={
            notifications.content.filter((notification) => !notification.seen)
              .length
              ? {
                  '.MuiBadge-badge': {
                    background: theme.colors.success.main,
                    animation: 'pulse 1s infinite',
                    transition: `${theme.transitions.create(['all'])}`
                  }
                }
              : {}
          }
        >
          <IconButtonWrapper
            sx={{
              background: alpha(theme.colors.primary.main, 0.1),
              transition: `${theme.transitions.create(['background'])}`,
              color: theme.colors.primary.main,

              '&:hover': {
                background: alpha(theme.colors.primary.main, 0.2)
              }
            }}
            color="primary"
            ref={ref}
            onClick={handleOpen}
          >
            <NotificationsActiveTwoToneIcon fontSize="small" />
          </IconButtonWrapper>
        </Badge>
      </Tooltip>
      <Popover
        disableScrollLock
        anchorEl={ref.current}
        onClose={handleClose}
        open={isOpen}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right'
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right'
        }}
      >
        <Box minWidth={440} maxWidth={440} p={1}>
          <BoxComposed
            mb={1}
            sx={{
              borderRadius: `${theme.general.borderRadius}`,
              background: `${theme.colors.gradients.black1}`
            }}
          >
            <BoxComposedBg
              sx={{
                opacity: 0.3,
                background: `${theme.colors.gradients.green2}`
              }}
            />
            <BoxComposedImage
              sx={{
                opacity: 0.2,
                backgroundImage:
                  'url("/static/images/placeholders/covers/1.jpg")'
              }}
            />
            <BoxComposedContent py={3}>
              <Typography
                textAlign="center"
                sx={{
                  pb: 0.5
                }}
                variant="h4"
              >
                {t('Notifications')}
              </Typography>
              <Typography textAlign="center" variant="subtitle2">
                {t('you_have')}{' '}
                <Text color="success">
                  <b>
                    {
                      notifications.content.filter(
                        (notification) => !notification.seen
                      ).length
                    }
                  </b>
                </Text>{' '}
                {t('new_messages')}
              </Typography>
            </BoxComposedContent>
          </BoxComposed>
          {!!notifications.content.filter((notification) => !notification.seen)
            .length && (
            <Button
              onClick={() => dispatch(readAllNotifications())}
              startIcon={<CheckCircleOutlineIcon />}
            >
              {t('mark_all_as_seen')}
            </Button>
          )}
        </Box>
        <Divider />
        {!!notifications.content.length && (
          <Box
            sx={{
              height: 220
            }}
          >
            {loadingGet && (
              <CircularProgress
                sx={{
                  position: 'absolute',
                  zIndex: 10,
                  left: '45%',
                  top: '45%'
                }}
              />
            )}
            <Scrollbar>
              <List onScroll={onScroll} sx={{ position: 'relative' }}>
                {notifications.content.map((notification) => (
                  <ListItemButton
                    selected={!notification.seen}
                    key={notification.id}
                    onClick={() => onReadNotification(notification)}
                  >
                    <ListItemIcon>
                      {cloneElement(
                        notificationIcons[notification.notificationType],
                        { color: notification.seen ? undefined : 'primary' }
                      )}
                    </ListItemIcon>
                    <ListItemText
                      primary={notification.message}
                      secondary={getFormattedDate(notification.createdAt)}
                    />
                  </ListItemButton>
                ))}
              </List>
            </Scrollbar>
          </Box>
        )}
        <Divider />
      </Popover>
    </>
  );
}

export default HeaderNotifications;
