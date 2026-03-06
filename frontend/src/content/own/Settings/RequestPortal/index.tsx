import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import { RequestPortal } from '../../../../models/owns/requestPortal';
import RequestPortalTable from './components/RequestPortalTable';
import { getSingleRequestPortal } from '../../../../slices/requestPortal';
import { useDispatch } from '../../../../store';

function RequestPortalSettings() {
  const { t }: { t: any } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [openModal, setOpenModal] = useState<boolean>(false);
  const [currentPortal, setCurrentPortal] = useState<
    RequestPortal | undefined
  >();
  const [activeTab, setActiveTab] = useState<'edit' | 'preview'>('edit');
  const dispatch = useDispatch();

  useEffect(() => {
    const useIdParam = async () => {
      if (id) {
        // Open modal in preview mode when URL has an ID
        const portal = (await dispatch(
          getSingleRequestPortal(Number(id))
        )) as unknown as RequestPortal;
        setActiveTab('preview');
        setCurrentPortal(portal);
        setOpenModal(true);
      }
    };
    useIdParam();
  }, [id]);

  const handleOpenModal = (
    portal?: RequestPortal,
    tab: 'edit' | 'preview' = 'edit'
  ) => {
    setCurrentPortal(portal);
    setActiveTab(tab);
    setOpenModal(true);
  };

  const handleCloseModal = () => {
    setOpenModal(false);
    setCurrentPortal(undefined);
    setActiveTab('edit');
    // Clear the URL parameter
    navigate('/app/settings/request-portals', { replace: true });
  };

  return (
    <RequestPortalTable
      openModal={openModal}
      currentPortal={currentPortal}
      activeTab={activeTab}
      onCloseModal={handleCloseModal}
      onOpenModal={handleOpenModal}
    />
  );
}

export default RequestPortalSettings;
