import {
  Box,
  Button,
  Card,
  Container,
  Stack,
  styled,
  Typography,
  List,
  ListItem
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import Logo from 'src/components/LogoSign';
import LanguageSwitcher from 'src/layouts/ExtendedSidebarLayout/Header/Buttons/LanguageSwitcher';
import NavBar from '../../components/NavBar';

function Overview() {
  const { t }: { t: any } = useTranslation();

  return (
    <Box>
      <Helmet>
        <title>{t('privacy_policy')}</title>
      </Helmet>
      <NavBar />
      <Container maxWidth="lg" sx={{ mt: 8 }}>
        <Box textAlign="center" mb={6}>
          <Typography variant="h1" component="h1" gutterBottom>
            {t('privacy_policy')}
          </Typography>
        </Box>
        <Card sx={{ mx: { md: 10, xs: 1 }, padding: { xs: 2, md: 4 } }}>
          <Typography variant="subtitle1" sx={{ mb: 3 }}>
            <strong>Effective Date:</strong> April 10, 2025
          </Typography>

          <Typography variant="h3" sx={{ mb: 2, fontWeight: 'bold' }}>
            1. Introduction
          </Typography>
          <Typography paragraph>
            Welcome to Atlas CMMS, a solution developed and provided by
            Intelloop s.a.r.l ("Intelloop", "we", "us", or "our"), located at
            410, Boulevard Zerktouni, Hamad, №1- Casablanca-Morocco 20040 (Trade
            Register: 585917, Tax ID: 53800712, ICE: 003298628000019). Intelloop
            s.a.r.l Au
          </Typography>
          <Typography paragraph>
            Our commitment is to your privacy and the protection of your
            information. This Privacy Policy outlines how we collect, use,
            disclose, and safeguard your information when you visit our website
            www.atlas-cmms.com (the "Site") or use our Atlas CMMS software and
            services (collectively, "Services").
          </Typography>
          <Typography paragraph>
            By accessing or using our Services, you signify your agreement to
            this Privacy Policy. If you do not agree with our policies and
            practices, please do not use our Services.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            2. Information We Collect
          </Typography>
          <Typography paragraph>
            We may collect personal information that identifies you as an
            individual or relates to an identifiable individual ("Personal
            Information"), including but not limited to:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Contact Information</strong>: Your name, email address
                and phone number
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Professional Information</strong>: Your company name.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Usage Data</strong>: Information on how you use our Site
                and Services, including IP addresses, browser type, access
                times, and pages viewed.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Account Information</strong>: Usernames, passwords, and
                other security information for authentication and access.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Technical Data</strong>: Device information, operating
                system, browser type, and other technical identifiers.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Location Data</strong>: General location information
                derived from IP addresses.
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            3. Legal Basis for Processing
          </Typography>
          <Typography paragraph>
            We process your personal information on the following legal bases:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Consent</strong>: When you explicitly agree to our
                Privacy Policy and Terms of Service, including when you opt-in
                to receive marketing communications.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Contractual Necessity</strong>: When processing is
                necessary for performing our contract with you when you use
                Atlas CMMS.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Legitimate Interests</strong>: When we have a legitimate
                business interest in ensuring the security, functionality, and
                improvement of our Services.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Legal Obligations</strong>: When we need to comply with
                legal obligations under Moroccan law and other applicable
                regulations.
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            4. How We Use Your Information
          </Typography>
          <Typography paragraph>
            We use your Personal Information for various purposes, including to:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Provide, maintain, and improve our Services.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Process and complete transactions, and send related information
                including confirmations and invoices.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Communicate with you about your account, our Services, and
                customer support.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Send technical notices, updates, security alerts, and
                administrative messages.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Respond to your comments, questions, and requests.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Enhance the security and safety of our Services.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Conduct research and analysis to understand how our Services are
                used.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Develop new products, services, features, and functionality.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Comply with legal obligations and enforce our terms and
                conditions.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>Prevent fraudulent or illegal activities.</Typography>
            </ListItem>
          </List>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            5. Data Retention Periods
          </Typography>
          <Typography paragraph>
            We retain different categories of data for specific periods:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Account Information</strong>: For the duration of your
                account plus 3 years after deletion
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Usage Data</strong>: For 12 months from collection
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Communication Records</strong>: For 3 years from last
                communication
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Billing Information</strong>: For 10 years as required
                by Moroccan tax law
              </Typography>
            </ListItem>
          </List>
          <Typography paragraph>
            After these periods expire, we will securely delete or anonymize
            your data unless a longer retention period is required by law.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            6. Sharing Your Information
          </Typography>
          <Typography paragraph>
            We do not sell your Personal Information. We may share your
            information with:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Service Providers</strong>: Third-party vendors and
                service providers who perform services on our behalf, including:
              </Typography>
              <List sx={{ pl: 4, listStyleType: 'circle' }}>
                <ListItem sx={{ display: 'list-item' }}>
                  <Typography>
                    Cloudflare Server (United States): Cloud hosting provider
                    for our infrastructure
                  </Typography>
                </ListItem>
                <ListItem sx={{ display: 'list-item' }}>
                  <Typography>
                    Netlify (Germany): Web Hosting Provider
                  </Typography>
                </ListItem>
                <ListItem sx={{ display: 'list-item' }}>
                  <Typography>
                    Google (United States): Email communications and Traffic
                    Analysis Tool
                  </Typography>
                </ListItem>
              </List>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Legal Requirements</strong>: Law enforcement, government
                authorities, or other third parties when required by law, to
                protect our rights, or to protect your safety or the safety of
                others.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Business Transfers</strong>: In connection with a
                merger, acquisition, or asset sale of our business, in which
                case the privacy policy of the new entity will govern.
              </Typography>
            </ListItem>
          </List>
          <Typography paragraph>
            For a complete and up-to-date list of our subprocessors, please
            visit: www.atlas-cmms.com/subprocessors
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            7. International Transfers
          </Typography>
          <Typography paragraph>
            Your Personal Information may be transferred to, and processed in,
            countries other than the country in which you are resident. These
            countries may have data protection laws that are different from the
            laws of your country.
          </Typography>
          <Typography paragraph>
            When transferring your data outside Morocco, we implement the
            following safeguards:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Standard Contractual Clauses approved by the European Commission
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Prior authorization from the CNDP for transfers to countries
                without adequate protection
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                End-to-end encryption during all data transfers
              </Typography>
            </ListItem>
          </List>
          <Typography paragraph>
            You may request a copy of these safeguards by contacting
            contact@atlas-cmms.com.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            8. Data Security
          </Typography>
          <Typography paragraph>
            We implement appropriate technical and organizational measures to
            protect your Personal Information against unauthorized or unlawful
            processing, accidental loss, destruction, or damage. These measures
            include:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>AES-256 encryption for all data at rest</Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                TLS 1.3 encryption for all data in transit
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Regular security training for all employees
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Documented incident response procedures with 72-hour
                notification timeline
              </Typography>
            </ListItem>
          </List>
          <Typography paragraph>
            While we strive to use commercially acceptable means to protect your
            Personal Information, no method of transmission over the Internet or
            electronic storage is 100% secure. We cannot guarantee absolute
            security.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            9. Your Rights
          </Typography>
          <Typography paragraph>
            Under Moroccan Law 09-08 and other applicable data protection laws,
            you have certain rights regarding your Personal Information,
            including:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Right to Access</strong>: The right to request a copy of
                the Personal Information we hold about you.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Right to Rectification</strong>: The right to request
                correction of inaccurate Personal Information.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Right to Erasure</strong>: The right to request deletion
                of your Personal Information in certain circumstances.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Right to Restrict Processing</strong>: The right to
                request restriction of processing of your Personal Information.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Right to Data Portability</strong>: The right to receive
                your Personal Information in a structured, commonly used format.
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Right to Object</strong>: The right to object to
                processing of your Personal Information in certain
                circumstances.
              </Typography>
            </ListItem>
          </List>
          <Typography paragraph>To exercise these rights, please:</Typography>
          <List sx={{ pl: 4 }}>
            <ListItem>
              <Typography>
                Email your request to contact@atlas-cmms.com
              </Typography>
            </ListItem>
            <ListItem>
              <Typography>
                Include your full name and email associated with your account
              </Typography>
            </ListItem>
            <ListItem>
              <Typography>Specify which right you wish to exercise</Typography>
            </ListItem>
            <ListItem>
              <Typography>
                We will respond within 10 days and may request additional
                verification
              </Typography>
            </ListItem>
            <ListItem>
              <Typography>
                If your request is denied, you may appeal to the CNDP
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            10. Cookies and Tracking Technologies
          </Typography>
          <Typography paragraph>
            We use cookies and similar tracking technologies to track activity
            on our Services and hold certain information. Cookies are files with
            a small amount of data that may include an anonymous unique
            identifier.
          </Typography>
          <Typography paragraph>
            We use the following types of cookies:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Essential Cookies</strong>: Necessary for basic
                functionality (session duration)
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Functional Cookies</strong>: Remember your preferences
                (1 year)
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Analytical Cookies</strong>: Help us improve our
                services (90 days)
              </Typography>
            </ListItem>
          </List>
          <Typography paragraph>
            You can manage your cookie preferences at any time through your
            account settings or by adjusting your browser settings. However, if
            you disable cookies, some features of our Services may not function
            properly.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            11. Children's Privacy
          </Typography>
          <Typography paragraph>
            Our Services are not intended for children under the age of 16. We
            do not knowingly collect personal information from children under
            16. If you are a parent or guardian and believe your child has
            provided us with Personal Information, please contact us at
            contact@atlas-cmms.com, and we will take steps to delete such
            information.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            12. Changes to This Privacy Policy
          </Typography>
          <Typography paragraph>
            We may update this Privacy Policy from time to time. We will notify
            you of any changes by:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Posting the new Privacy Policy on this page
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Sending an email notification to the address associated with
                your account
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>Providing an in-app notification</Typography>
            </ListItem>
          </List>
          <Typography paragraph>
            Changes will be effective 30 days after notification for material
            changes. Your continued use of our Services after this period
            constitutes acceptance of the updated Privacy Policy.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            13. Contact Us
          </Typography>
          <Typography paragraph>
            If you have any questions about this Privacy Policy, please contact
            us at:
          </Typography>
          <Typography sx={{ mb: 1 }}>
            <strong>Intelloop s.a.r.l</strong>
          </Typography>
          <Typography>410, Boulevard Zerktouni, Hamad, №1</Typography>
          <Typography>Casablanca-Morocco 20040</Typography>
          <Typography>Email: med.labiad@intel-loop.com</Typography>
          <Typography sx={{ mb: 3 }}>Phone: +212 6 30 69 00 50</Typography>
          <Typography>
            You also have the right to lodge a complaint with the CNDP if you
            believe that we have violated your data protection rights.
          </Typography>
        </Card>
      </Container>
    </Box>
  );
}

export default Overview;
