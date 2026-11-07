import AnalyticsLayout from '../AnalyticsLayout';
import WOModal, { Filter } from './WOModal';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Status from './Status';
import Analysis from './Analysis';
import Aging from './Aging';
import TimeAndCost from './TimeAndCost';
import Kpis from './Kpis';

export default function Index() {
  const { t }: { t: any } = useTranslation();
  const [woModalTitle, setWoModalTitle] = useState<string>(t('work_orders'));
  const [openWOModal, setOpenWOModal] = useState<boolean>(false);
  const [columns, setColumns] = useState<string[]>([]);
  const [filters, setFilters] = useState<Filter[]>([]);
  const handleOpenWOModal = (
    columns: string[],
    filters: Filter[],
    title: string
  ) => {
    return;// TODO
    setColumns(columns);
    setFilters(filters);
    setWoModalTitle(title);
    setOpenWOModal(true);
  };
  const renderComponent = () => {
    const arr = location.pathname.split('/');
    switch (arr[arr.length - 1]) {
      case 'status':
        return <Status handleOpenWOModal={handleOpenWOModal} />;
      case 'analysis':
        return <Analysis handleOpenWOModal={handleOpenWOModal} />;
      case 'aging':
        return <Aging handleOpenWOModal={handleOpenWOModal} />;
      case 'time-cost':
        return <TimeAndCost handleOpenWOModal={handleOpenWOModal} />;
      case 'kpis':
        return <Kpis />;
      default:
        return null;
    }
  };
  return (
    <AnalyticsLayout>
      {renderComponent()}
      <WOModal
        title={woModalTitle}
        open={openWOModal}
        onClose={() => setOpenWOModal(false)}
        columns={columns}
        filters={filters}
      />
    </AnalyticsLayout>
  );
}
