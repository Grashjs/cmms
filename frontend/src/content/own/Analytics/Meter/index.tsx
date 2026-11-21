import AnalyticsLayout from '../AnalyticsLayout';
import MeterTrends from './Trends';

export default function Index() {
  const renderComponent = () => {
    const arr = location.pathname.split('/');
    switch (arr[arr.length - 1]) {
      case 'trends':
        return <MeterTrends />;
      default:
        return null;
    }
  };
  return <AnalyticsLayout>{renderComponent()}</AnalyticsLayout>;
}
