import Hero from "./Hero";
import Highlights from "./Highlights";
import NavBar from "src/components/NavBar";
import Footer from "src/components/Footer";
import CompanyLogos from "src/components/CompanyLogos";
import { Box } from "@mui/material";

function Overview() {
  return (
    <Box
      sx={{
        overflow: "auto",
        background: "#ffffff",
        flex: 1,
        overflowX: "hidden",
      }}
    >
      <NavBar />
      <Hero />
      <CompanyLogos sx={{ mt: { xs: "150px", md: "100px" } }} />
      <Highlights />
      <Footer />
    </Box>
  );
}

export default Overview;
