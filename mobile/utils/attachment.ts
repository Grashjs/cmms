import { Alert, Linking } from 'react-native';
import i18n from 'i18next';
import { URL } from 'react-native-url-polyfill';

/** Open the protected HTTP response externally; never render attachments in an app WebView. */
export async function openAttachment(value: string): Promise<boolean> {
  try {
    const url = new URL(value);
    if (!['https:', 'http:'].includes(url.protocol)) {
      throw new Error('Invalid attachment URL');
    }
    await Linking.openURL(url.href);
    return true;
  } catch {
    // No logging of signed bearer URLs.
    Alert.alert(i18n.t('error'), i18n.t('an_error_occured'));
    return false;
  }
}
