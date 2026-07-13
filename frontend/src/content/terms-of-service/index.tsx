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

function TermsOfService() {
  const { t }: { t: any } = useTranslation();

  return (
    <Box>
      <Helmet>
        <title>{t('terms_of_service')}</title>
      </Helmet>
      <NavBar />
      <Container maxWidth="lg" sx={{ mt: 8 }}>
        <Box textAlign="center" mb={6}>
          <Typography variant="h1" component="h1" gutterBottom>
            {t('terms_of_service')}
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
            This Terms of Service Agreement ("Agreement") is a legal agreement
            between you as an individual or entity ("You" or "Customer") and
            Intelloop s.a.r.l located at 410, Boulevard Zerktouni, Hamad, N°1-
            Casablanca-Morocco 20040, Trade Register: 585917, Tax Id: 53800712,
            ICE: 003298628000019 ("Company", "we", "us" or "our"), a
            Morocco-based management consultancy specializing in industrial
            Projects launch, Maintenance and Reliability, governing your use of
            the Atlas CMMS software and services (the "Software").
          </Typography>
          <Typography paragraph sx={{ fontWeight: 'bold' }}>
            BY ACCEPTING THIS AGREEMENT OR BY ACCESSING OR USING THE SOFTWARE,
            YOU AGREE TO BE BOUND BY THE TERMS OF THIS AGREEMENT. IF YOU ARE
            ENTERING INTO THIS AGREEMENT ON BEHALF OF A COMPANY OR OTHER LEGAL
            ENTITY, YOU REPRESENT THAT YOU HAVE THE AUTHORITY TO BIND SUCH
            ENTITY TO THIS AGREEMENT.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            2. Service Description
          </Typography>
          <Typography paragraph>
            Atlas CMMS is a solution developed and provided by Intelloop s.a.r.l
            as part of its ADDAPTIVE Tech services, which guide companies
            through digital transformation in maintenance and reliability. The
            Software is offered as a service that may be deployed through
            self-hosted, cloud-based, or hybrid models.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            3. Free Trial
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            3.1 Trial Period
          </Typography>
          <Typography paragraph>
            Company grants you a limited, non-exclusive, non-transferable right
            to access and use the Software for a period of 15 days from
            acceptance of this Agreement (the "Trial Period") solely for the
            purpose of internally evaluating the suitability of the Software for
            your business needs.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            3.2 Trial Limitations
          </Typography>
          <Typography paragraph>
            You may not use the Software for any production or commercial
            purposes during the Trial Period.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            3.3 Trial Expiration
          </Typography>
          <Typography paragraph>
            Upon expiration of the Trial Period:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                You will receive email notifications at 7 days, 3 days, and 1
                day before expiration
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Your access to the Software will terminate automatically unless
                you purchase a paid subscription
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Your data will be retained for 30 days after trial expiration
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                You can convert to a paid subscription at any time during this
                period
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                If you do not purchase a paid subscription within 30 days of the
                end of the Trial Period, all of your data in the Software may be
                permanently deleted
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                If you choose not to convert, you may export your data in
                standard formats before the 30-day retention period ends
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            4. Subscription Terms
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            4.1 Subscription Models
          </Typography>
          <Typography paragraph>
            Following the Trial Period, we offer several subscription models:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Self-Hosted:</strong> Deploy Atlas CMMS on your own
                servers with complete control over your data and environment
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Cloud-Based:</strong> Let us handle the hosting,
                updates, and maintenance while you focus on your core operations
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                <strong>Hybrid:</strong> Combine self-hosted core components
                with cloud-based services for a tailored solution
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            4.2 Subscription Fees
          </Typography>
          <Typography paragraph>
            Subscription fees are based on the selected deployment model and
            number of users. Current pricing is available at
            www.atlas-cmms.com/pricing.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            4.3 Payment Terms
          </Typography>
          <Typography paragraph>
            All subscription fees are payable in advance and non-refundable. You
            agree to provide accurate billing information and authorize us to
            charge your payment method for all subscriptions.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            4.4 Subscription Renewal
          </Typography>
          <Typography paragraph>
            Subscriptions automatically renew for the same term unless cancelled
            at least 30 days before the renewal date.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            5. Service Level Agreement
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            5.1 Uptime Commitment
          </Typography>
          <Typography paragraph>We commit to:</Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                99.9% uptime calculated on a monthly basis for cloud-based
                deployments
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Scheduled maintenance during off-peak hours with 48-hour advance
                notice
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Immediate notification of unplanned downtime via email and
                status page
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            5.2 Support Services
          </Typography>
          <Typography paragraph>
            We provide technical support as specified in your subscription plan.
            Standard support includes email support during business hours with a
            24-hour response time.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            6. Restrictions and Acceptable Use
          </Typography>
          <Typography paragraph>
            You agree to use the Software only for lawful purposes and in
            compliance with this Agreement. Except as expressly permitted, you
            shall not, and shall not permit any third party to:
          </Typography>
          <Typography paragraph sx={{ pl: 3 }}>
            a. Copy, modify, adapt, translate or otherwise create derivative
            works of the Software;
            <br />
            b. Reverse engineer, decompile, disassemble or otherwise attempt to
            discover the source code or underlying ideas or algorithms of the
            Software;
            <br />
            c. Rent, lease, distribute, sell, sublicense, assign or otherwise
            transfer rights to the Software;
            <br />
            d. Remove any proprietary notices from the Software;
            <br />
            e. Use the Software for the purpose of building a competitive
            product or service or copying its features or user interface;
            <br />
            f. Use the Software to send spam, or otherwise engage in unlawful or
            tortious activities;
            <br />
            g. Interfere with or disrupt the integrity or performance of the
            Software;
            <br />
            h. Attempt to gain unauthorized access to the Software or related
            systems or networks;
            <br />
            i. Introduce any viruses, Trojan horses, worms, logic bombs or other
            material which is malicious or harmful;
            <br />
            j. Permit direct or indirect access to or use of the Software in any
            manner that circumvents contractual usage limitations.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            7. Intellectual Property Rights
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            7.1 Ownership
          </Typography>
          <Typography paragraph>
            The Software, Atlas CMMS Logo, and all worldwide intellectual
            property rights therein are the exclusive property of Company.
            Intelloop s.a.r.l owns all intellectual property rights to Atlas
            CMMS, including its codebase, design, and documentation, except for
            open source components which are governed by their respective
            licenses. While Atlas CMMS is open source, your use is subject to
            the terms of this Agreement.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            7.2 License Grant
          </Typography>
          <Typography paragraph>
            Company grants you a limited, non-exclusive, non-transferable
            license to use the Software during the subscription term solely for
            your internal business purposes.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            7.3 Open Source Components
          </Typography>
          <Typography paragraph>
            The Software includes certain open source components, which are
            subject to their respective open source licenses. A list of these
            components and their licenses is available at
            https://github.com/Grashjs/cmms
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            7.4 User-Generated Content
          </Typography>
          <Typography paragraph>
            You retain ownership of any content you upload to the Software. You
            grant Company a non-exclusive, royalty-free license to use,
            reproduce, modify, and display such content solely for the purpose
            of providing and improving the Software.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            8. Confidentiality
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            8.1 Definition
          </Typography>
          <Typography paragraph>
            "Confidential Information" means all non-public information
            disclosed by either party to the other, whether orally or in
            writing, that is designated as confidential or that reasonably
            should be understood to be confidential given the nature of the
            information and the circumstances of disclosure.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            8.2 Obligations
          </Typography>
          <Typography paragraph>Each party agrees to:</Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Maintain the confidentiality of the other party's Confidential
                Information
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Use the same degree of care to protect the confidentiality of
                the other party's Confidential Information that it uses to
                protect its own Confidential Information, but in no event less
                than reasonable care
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Not disclose such Confidential Information to any third party,
                except as required by law or as necessary to fulfill the
                purposes of this Agreement
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            8.3 Exclusions
          </Typography>
          <Typography paragraph>
            Confidential Information does not include information that:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Is or becomes generally known to the public without breach of
                this Agreement
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Was known to the receiving party prior to its disclosure by the
                disclosing party
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Is received from a third party without breach of any obligation
                owed to the disclosing party
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Was independently developed by the receiving party without use
                of the disclosing party's Confidential Information
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            9. Data Protection and Privacy
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            9.1 Data Processing
          </Typography>
          <Typography paragraph>
            Company shall handle any personal data collected from users in
            accordance with its Privacy Policy available at
            www.atlas-cmms.com/privacy
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            9.2 Security Measures
          </Typography>
          <Typography paragraph>
            Company shall implement appropriate technical and organizational
            measures to protect user data, including:
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
                Regular security audits and penetration testing
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            9.3 Data Breach Notification
          </Typography>
          <Typography paragraph>
            In the event of a data breach affecting your data, we will notify
            you within 72 hours of discovery and take appropriate measures to
            mitigate the impact.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            10. Warranties and Disclaimers
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            10.1 Limited Warranty
          </Typography>
          <Typography paragraph>
            Company warrants that the Software will perform materially in
            accordance with the documentation during the subscription term.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            10.2 Disclaimer
          </Typography>
          <Typography paragraph>
            EXCEPT AS EXPRESSLY PROVIDED HEREIN, THE SOFTWARE IS PROVIDED "AS
            IS" AND "AS AVAILABLE." COMPANY DISCLAIMS ALL WARRANTIES, EXPRESS OR
            IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF
            MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND
            NON-INFRINGEMENT.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            10.3 Internet Delays
          </Typography>
          <Typography paragraph>
            THE SOFTWARE MAY BE SUBJECT TO LIMITATIONS, DELAYS, AND OTHER
            PROBLEMS INHERENT IN THE USE OF THE INTERNET AND ELECTRONIC
            COMMUNICATIONS. COMPANY IS NOT RESPONSIBLE FOR ANY DELAYS, DELIVERY
            FAILURES, OR OTHER DAMAGE RESULTING FROM SUCH PROBLEMS.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            11. Limitation of Liability
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            11.1 Exclusion of Damages
          </Typography>
          <Typography paragraph>
            IN NO EVENT SHALL COMPANY BE LIABLE FOR ANY INDIRECT, INCIDENTAL,
            SPECIAL, CONSEQUENTIAL OR PUNITIVE DAMAGES, OR DAMAGES FOR LOSS OF
            PROFITS, REVENUE, DATA OR USE, INCURRED BY YOU OR ANY THIRD PARTY,
            WHETHER IN CONTRACT, TORT OR OTHERWISE, EVEN IF COMPANY HAS BEEN
            ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            11.2 Limitation of Liability
          </Typography>
          <Typography paragraph>
            COMPANY'S TOTAL LIABILITY ARISING OUT OF OR RELATED TO THIS
            AGREEMENT SHALL NOT EXCEED THE AMOUNT PAID BY YOU FOR THE SOFTWARE
            DURING THE TWELVE (12) MONTHS PRECEDING THE EVENT GIVING RISE TO THE
            LIABILITY.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            11.3 Essential Purpose
          </Typography>
          <Typography paragraph>
            THE LIMITATIONS OF LIABILITY IN THIS SECTION SHALL APPLY EVEN IF ANY
            LIMITED REMEDY FAILS OF ITS ESSENTIAL PURPOSE.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            12. Indemnification
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            12.1 By Customer
          </Typography>
          <Typography paragraph>
            You agree to indemnify and hold Company harmless from any claim,
            demand, loss, liability, damage or expense (including reasonable
            attorneys' fees) arising out of your use of the Software or
            violation of this Agreement.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            12.2 By Company
          </Typography>
          <Typography paragraph>
            Company agrees to defend you against any claim by a third party
            alleging that the Software infringes any intellectual property
            right, and to indemnify you for any damages finally awarded against
            you in connection with such claim, provided that you promptly notify
            Company of the claim, allow Company to control the defense, and
            cooperate with Company in the defense.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            13. Term and Termination
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            13.1 Term
          </Typography>
          <Typography paragraph>
            This Agreement commences on the date you first accept it and
            continues until all subscriptions hereunder have expired or been
            terminated.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            13.2 Termination for Cause
          </Typography>
          <Typography paragraph>
            Either party may terminate this Agreement for cause if the other
            party materially breaches this Agreement and fails to cure such
            breach within thirty (30) days after receiving written notice of the
            breach.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            13.3 Effect of Termination
          </Typography>
          <Typography paragraph>
            Upon termination or expiration of this Agreement:
          </Typography>
          <List sx={{ pl: 4, listStyleType: 'disc' }}>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                All licenses granted hereunder will immediately terminate
              </Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>You must cease all use of the Software</Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>You must pay all outstanding fees</Typography>
            </ListItem>
            <ListItem sx={{ display: 'list-item' }}>
              <Typography>
                Company will make your data available for export for a period of
                30 days, after which it may be deleted
              </Typography>
            </ListItem>
          </List>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            13.4 Survival
          </Typography>
          <Typography paragraph>
            Sections 7, 8, 11, 12, 13.3, 13.4, and 14 shall survive termination
            or expiration of this Agreement.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            14. Modifications to the Software and Agreement
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            14.1 Software Modifications
          </Typography>
          <Typography paragraph>
            Company reserves the right to modify or discontinue the Software at
            any time. For material changes, we will provide at least 90 days'
            notice via email and in-app notification.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            14.2 Agreement Modifications
          </Typography>
          <Typography paragraph>
            Company may update this Agreement from time to time. We will notify
            you of material changes at least 30 days before they become
            effective via email and in-app notification. Your continued use of
            the Software after the effective date constitutes acceptance of the
            modified terms. If you do not agree to the changes, you must stop
            using the Software and may terminate your account, subject to the
            terms of this Agreement.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            15. General Provisions
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            15.1 Governing Law
          </Typography>
          <Typography paragraph>
            This Agreement shall be governed by the laws of Morocco without
            regard to its conflict of law principles.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            15.2 Dispute Resolution
          </Typography>
          <Typography paragraph>
            Any disputes arising out of or relating to this Agreement shall be
            resolved through binding arbitration in Casablanca, Morocco,
            administered by the Moroccan Arbitration Center under its Commercial
            Arbitration Rules. The arbitration panel shall consist of one
            arbitrator selected in accordance with these rules. The arbitration
            must be commenced within one year after the dispute arises. Nothing
            in this section prevents either party from seeking injunctive relief
            in a court of competent jurisdiction.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            15.3 Force Majeure
          </Typography>
          <Typography paragraph>
            Neither party shall be liable for any delay or failure to perform
            its obligations under this Agreement due to causes beyond its
            reasonable control, including but not limited to acts of God,
            natural disasters, war, terrorism, riots, civil unrest, government
            action, strikes, lockouts, labor disputes, fire, explosion, or power
            outages.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            15.4 Assignment
          </Typography>
          <Typography paragraph>
            You may not assign or transfer this Agreement or any rights or
            obligations hereunder without the prior written consent of Company.
            Company may freely assign this Agreement. Any attempted assignment
            in violation of this section shall be void.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            15.5 Severability
          </Typography>
          <Typography paragraph>
            If any provision of this Agreement is held to be invalid or
            unenforceable, such provision shall be struck and the remaining
            provisions shall remain in full force and effect.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            15.6 No Waiver
          </Typography>
          <Typography paragraph>
            The failure of Company to exercise or enforce any right or provision
            of this Agreement shall not constitute a waiver of such right or
            provision.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            15.7 Entire Agreement
          </Typography>
          <Typography paragraph>
            This Agreement, together with the Privacy Policy and any other
            documents referenced herein, constitutes the entire agreement
            between the parties and supersedes all prior communications and
            agreements. It may only be amended as provided in Section 14.2.
          </Typography>

          <Typography variant="h4" sx={{ mt: 2, mb: 1, fontWeight: 'bold' }}>
            15.8 Notices
          </Typography>
          <Typography paragraph>
            Any notices required under this Agreement shall be provided to users
            by email or through the Software interface. Users shall send any
            notices to Company at contact@atlas-cmms.com.
          </Typography>

          <Typography variant="h3" sx={{ mt: 3, mb: 2, fontWeight: 'bold' }}>
            16. Contact Information
          </Typography>
          <Typography paragraph>
            If you have any questions about this Agreement, please contact us
            at:
          </Typography>
          <Typography sx={{ mb: 1 }}>
            <strong>Intelloop s.a.r.l</strong>
          </Typography>
          <Typography>410, Boulevard Zerktouni, Hamad, №1</Typography>
          <Typography>Casablanca-Morocco 20040</Typography>
          <Typography>
            Email: med.labiad@intel-loop.com/contact@atlas-cmms.com
          </Typography>
          <Typography sx={{ mb: 3 }}>Phone: +212 6 30 69 00 50</Typography>

          <Typography paragraph sx={{ mt: 4, fontStyle: 'italic' }}>
            By using the Atlas CMMS software, you acknowledge that you have
            read, understood and agree to be bound by this Agreement.
          </Typography>
        </Card>
      </Container>
    </Box>
  );
}

export default TermsOfService;
