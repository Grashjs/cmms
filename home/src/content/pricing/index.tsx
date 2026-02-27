"use client";
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Container,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  OutlinedInput,
  Select,
  styled,
  Tab,
  Tabs,
  Typography,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import { Helmet } from "react-helmet-async";
import { useTranslations } from "next-intl";
import { useSearchParams } from "next/navigation";
import { Link, usePathname, useRouter } from "src/i18n/routing";
import CheckCircleOutlineTwoToneIcon from "@mui/icons-material/CheckCircleOutlineTwoTone";
import { useEffect, useState } from "react";
import { getPlanFeatureCategories, getPricingPlans, getSelfHostedPlans } from "./pricingData";
import NavBar from "src/components/NavBar";
import Faq from "./components/Faq";
import SubscriptionPlanSelector, { PRICING_YEAR_MULTIPLIER } from "./components/SubscriptionPlanSelector";
import { fireGa4Event } from "src/utils/overall";
import Footer from "src/components/Footer";

const PricingWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`,
);

function Pricing() {
  const t = useTranslations();
  const theme = useTheme();
  const router = useRouter();
  const searchParams = useSearchParams();
  const typeParam = searchParams.get("type");
  const type = typeParam === "selfhosted" ? "selfhosted" : "cloud";
  const [monthly, setMonthly] = useState<boolean>(true);
  const typePlans = type === "cloud" ? getPricingPlans(t) : getSelfHostedPlans(t);
  const [selectedPlans, setSelectedPlans] = useState<string[]>([]);

  const isXs = useMediaQuery(theme.breakpoints.only("xs"));
  const isSm = useMediaQuery(theme.breakpoints.only("sm"));
  const isMdDown = useMediaQuery(theme.breakpoints.down("md"));
  const pathname = usePathname(); // returns path WITHOUT locale prefix

  const handleTabsChange = (_event: React.ChangeEvent<{}>, value: string): void => {
    router.push(`${pathname}?type=${value}`);
  };

  // Set default selected plans based on screen size
  useEffect(() => {
    // Find the popular plan
    const popularPlan = typePlans.find((plan) => plan.popular)?.id || typePlans[0].id;

    if (isXs) {
      // For extra small screens, select 2 plans (popular plan + one more)
      const secondPlan = typePlans.find((plan) => plan.id !== popularPlan)?.id || "";
      setSelectedPlans([popularPlan, secondPlan].filter(Boolean));
    } else if (isSm) {
      // For small screens, select 3 plans (popular plan + two more)
      const otherPlans = typePlans
        .filter((plan) => plan.id !== popularPlan)
        .slice(0, 2)
        .map((plan) => plan.id);
      setSelectedPlans([popularPlan, ...otherPlans]);
    } else {
      // For medium and up, show all plans
      setSelectedPlans(typePlans.map((plan) => plan.id));
    }
  }, [isXs, isSm, isMdDown, type]);

  useEffect(() => {
    fireGa4Event("pricing_view");
  }, []);
  return (
    <PricingWrapper>
      <Helmet>
        <title>Pricing - Atlas CMMS</title>
        <meta
          name="description"
          content="Flexible pricing plans for Atlas CMMS. Choose between Cloud and Self-Hosted versions of our open-source CMMS to optimize your maintenance operations."
        />
        <link rel="canonical" href="https://atlas-cmms.com/pricing" />
      </Helmet>
      <NavBar />

      <Container maxWidth="lg" sx={{ mt: 8 }}>
        <Box textAlign="center" mb={6}>
          <Typography variant="h1" component="h1" gutterBottom>
            {t("pricing_1.choose_plan_and_get_started")}
          </Typography>
          <Typography variant="subtitle1">{t("pricing_1.slogan_effective_maintenance")}</Typography>
        </Box>
        <Box sx={{ display: "flex", justifyContent: "center", mb: 4 }}>
          <Tabs value={type} onChange={handleTabsChange} indicatorColor="primary" textColor="primary">
            <Tab label={t("cloud")} value="cloud" />
            <Tab label={t("self_hosted")} value="selfhosted" />
          </Tabs>
        </Box>

        <SubscriptionPlanSelector monthly={monthly} setMonthly={setMonthly} selfHosted={type === "selfhosted"} />
        <Box textAlign="center" my={6}>
          <Typography variant="h1" component="h1" gutterBottom>
            {t("pricing_1.compare_plans_and_pricing")}
          </Typography>
          <Typography variant="subtitle1">{t("pricing_1.see_which_plan_is_right_for_you")}</Typography>

          {/* Plan selection dropdown for small/medium screens */}
          <Box
            sx={{
              mt: 3,
              display: { xs: "block", md: "none" },
              mx: "auto",
              maxWidth: { xs: "100%", sm: "80%" },
            }}
          >
            <FormControl fullWidth>
              <InputLabel id="plan-comparison-select-label">
                {isXs ? t("pricing_1.select_two_plans_to_compare") : t("pricing_1.select_three_plans_to_compare")}
              </InputLabel>
              <Select
                labelId="plan-comparison-select-label"
                id="plan-comparison-select"
                multiple
                value={selectedPlans}
                onChange={(e) => setSelectedPlans(e.target.value as string[])}
                input={
                  <OutlinedInput
                    label={
                      isXs ? t("pricing_1.select_two_plans_to_compare") : t("pricing_1.select_three_plans_to_compare")
                    }
                  />
                }
                renderValue={(selected) => (
                  <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5 }}>
                    {selected.map((value) => {
                      const plan = typePlans.find((p) => p.id === value);
                      return <Chip key={value} label={plan?.name} />;
                    })}
                  </Box>
                )}
                sx={{ mb: 2 }}
              >
                {typePlans.map((plan) => (
                  <MenuItem
                    key={plan.id}
                    value={plan.id}
                    disabled={selectedPlans.length >= (isXs ? 2 : 3) && !selectedPlans.includes(plan.id)}
                  >
                    {plan.name} {plan.popular && "✨"}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </Box>
        <Card>
          <CardContent sx={{ p: 4 }}>
            <Box>
              <Grid container>
                <Grid item xs={12} md={4}>
                  {/* Empty grid for alignment */}
                </Grid>
                {/* Filter plans based on selection for small/medium screens */}
                {typePlans
                  .filter((plan) => !isMdDown || selectedPlans.includes(plan.id))
                  .map((plan) => (
                    <Grid item xs={6} sm={4} md={2} key={`compare-header-${plan.id}`} sx={{ textAlign: "center" }}>
                      <Typography variant="h5" gutterBottom>
                        {plan.name}
                      </Typography>
                      {!parseFloat(plan.price) ? (
                        <Typography variant="body2" color="textSecondary">
                          {plan.price}
                        </Typography>
                      ) : (
                        <Typography variant="h6" color="primary">
                          ${monthly ? plan.price : parseFloat(plan.price) * PRICING_YEAR_MULTIPLIER}
                          {`/${monthly ? t("pricing_1.month_per_user") : t("pricing_1.year_per_user")}`}
                        </Typography>
                      )}
                      {type === "cloud" && (
                        <Button
                          size="small"
                          variant="outlined"
                          component={Link}
                          href={"/account/register" + (plan.id !== "basic" ? `?subscription-plan-id=${plan.id}` : "")}
                          sx={{ mt: 1, mb: 2 }}
                        >
                          {plan.id === "basic" ? t("get_started") : t("try_for_free")}
                        </Button>
                      )}
                    </Grid>
                  ))}
              </Grid>

              {getPlanFeatureCategories(t).map((category, categoryIndex) => (
                <Box key={`category-${categoryIndex}`} sx={{ mb: 4 }}>
                  <Typography variant="h6" sx={{ mb: 2, mt: 3, fontWeight: "bold" }}>
                    {category.name}
                  </Typography>

                  {category.features.map((feature, featureIndex) => (
                    <Grid
                      container
                      key={`feature-${categoryIndex}-${featureIndex}`}
                      sx={{
                        py: 1,
                        borderBottom: `1px solid ${theme.colors.alpha.black[10]}`,
                        backgroundColor: featureIndex % 2 ? "#F2F5F9" : "white",
                        "&:hover": {
                          backgroundColor: theme.colors.alpha.black[5],
                        },
                      }}
                    >
                      <Grid item xs={12} md={4}>
                        <Typography variant="body2">{feature.name}</Typography>
                      </Grid>

                      {typePlans
                        .filter((plan) => !isMdDown || selectedPlans.includes(plan.id))
                        .map((plan) => (
                          <Grid
                            item
                            xs={6}
                            sm={4}
                            md={2}
                            key={`feature-${categoryIndex}-${featureIndex}-${plan.id}`}
                            sx={{
                              textAlign: "center",
                              display: "flex",
                              justifyContent: "center",
                              alignItems: "center",
                            }}
                          >
                            {feature.availability[plan.id] === true && (
                              <CheckCircleOutlineTwoToneIcon fontSize={isMdDown ? "small" : "medium"} color="primary" />
                            )}
                            {feature.availability[plan.id] === false && <Typography variant="body2">–</Typography>}
                            {typeof feature.availability[plan.id] === "string" && (
                              <Typography variant="body2">{feature.availability[plan.id]}</Typography>
                            )}
                          </Grid>
                        ))}
                    </Grid>
                  ))}
                </Box>
              ))}
            </Box>
          </CardContent>
        </Card>
        <Faq />
      </Container>
    </PricingWrapper>
  );
}

export default Pricing;
