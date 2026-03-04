import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { RequestPortal } from '../../../../models/owns/requestPortal';
import RequestPortalTable from './components/RequestPortalTable';

function RequestPortalSettings() {
  const { t }: { t: any } = useTranslation();
  const [openModal, setOpenModal] = useState<boolean>(false);
  const [currentPortal, setCurrentPortal] = useState<RequestPortal | undefined>();

  const handleOpenModal = (portal?: RequestPortal) => {
    setCurrentPortal(portal);
    setOpenModal(true);
  };

  const handleCloseModal = () => {
    setOpenModal(false);
    setCurrentPortal(undefined);
  };

  return (
    <RequestPortalTable
      openModal={openModal}
      currentPortal={currentPortal}
      onCloseModal={handleCloseModal}
      onOpenModal={handleOpenModal}
    />
  );
}

export default RequestPortalSettings;
