import { ChangeEvent, FC, useContext, useState } from 'react';
import PropTypes from 'prop-types';
import type { OwnUser, UserResponseDTO } from 'src/models/user';
import {
  Avatar,
  Box,
  Card,
  CircularProgress,
  IconButton,
  styled,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import UploadTwoToneIcon from '@mui/icons-material/UploadTwoTone';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import useAuth from '../../../hooks/useAuth';

const Input = styled('input')({
  display: 'none'
});

const AvatarWrapper = styled(Card)(
  ({ theme }) => `
    position: relative;
    overflow: visible;
    display: inline-block;
    margin-left: ${theme.spacing(2)};
    margin-top: ${theme.spacing(1)};

    .MuiAvatar-root {
      width: ${theme.spacing(16)};
      height: ${theme.spacing(16)};
    }
`
);

const ButtonUploadWrapper = styled(Box)(
  ({ theme }) => `
    position: absolute;
    width: ${theme.spacing(4)};
    height: ${theme.spacing(4)};
    bottom: -${theme.spacing(1)};
    right: -${theme.spacing(1)};

    .MuiIconButton-root {
      border-radius: 100%;
      background: ${theme.colors.primary.main};
      color: ${theme.palette.primary.contrastText};
      box-shadow: ${theme.colors.shadows.primary};
      width: ${theme.spacing(4)};
      height: ${theme.spacing(4)};
      padding: 0;
  
      &:hover {
        background: ${theme.colors.primary.dark};
      }
    }
`
);

const CardCover = styled(Card)(
  ({ theme }) => `
    position: relative;

    .MuiCardMedia-root {
      height: ${theme.spacing(26)};
    }
`
);

const CardCoverAction = styled(Box)(
  ({ theme }) => `
    position: absolute;
    right: ${theme.spacing(2)};
    bottom: ${theme.spacing(2)};
`
);

interface ProfileCoverProps {
  user: UserResponseDTO;
}

const ProfileCover: FC<ProfileCoverProps> = ({ user }) => {
  const { t }: { t: any } = useTranslation();
  const [changingPicture, setChangingPicture] = useState<boolean>(false);
  const { uploadFiles } = useContext(CompanySettingsContext);
  const { patchUser } = useAuth();
  const onChangePicture = (event: ChangeEvent<HTMLInputElement>) => {
    setChangingPicture(true);
    uploadFiles([], Array.from(event.target.files), true)
      .then((files) =>
        patchUser({ image: { id: files[0].id } } as Partial<OwnUser>)
      )
      .finally(() => setChangingPicture(false));
  };
  return (
    <>
      <AvatarWrapper>
        <Avatar variant="rounded" src={user.image?.url} alt={user.firstName} />
        <ButtonUploadWrapper>
          <Input
            accept="image/*"
            id="icon-button-file"
            onChange={onChangePicture}
            disabled={changingPicture}
            name="icon-button-file"
            type="file"
          />
          <label htmlFor="icon-button-file">
            <IconButton component="span" color="primary">
              {changingPicture ? (
                <CircularProgress size="1rem" sx={{ color: 'white' }} />
              ) : (
                <UploadTwoToneIcon />
              )}
            </IconButton>
          </label>
        </ButtonUploadWrapper>
      </AvatarWrapper>
      <Box py={1} pl={2}>
        <Typography gutterBottom variant="h4">
          {user.firstName} {user.lastName}
        </Typography>
      </Box>
    </>
  );
};

ProfileCover.propTypes = {
  // @ts-ignore
  user: PropTypes.object.isRequired
};

export default ProfileCover;
