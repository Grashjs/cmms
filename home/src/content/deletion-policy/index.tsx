import { Box, Card, Typography } from "@mui/material";
import { getTranslations } from "next-intl/server";
import { getBrandServer } from "src/utils/serverBrand";
import NavBar from "src/components/NavBar";

async function DeletionPolicy() {
  const t = await getTranslations();
  const brandConfig = await getBrandServer();

  return (
    <Box>
      <NavBar />
      <Box sx={{ mx: 10, padding: 2 }}>
        <Typography variant={"h1"}>{t("account_deletion", { brandName: brandConfig.name })}</Typography>
      </Box>
      <Card sx={{ mx: 10, padding: 2, mb: 6 }}>
        <Typography>
          {
            "You are free to delete your Account at any time. To do that, you need to login go to >My account>Delete account"
          }
        </Typography>
      </Card>
    </Box>
  );
}

export default DeletionPolicy;
