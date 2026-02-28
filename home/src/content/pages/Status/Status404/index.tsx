import { Box, Card, Typography, Container, Divider, Button, FormControl, InputAdornment } from "@mui/material";
import SearchTwoToneIcon from "@mui/icons-material/SearchTwoTone";
import { useTranslations } from "next-intl";
import { MainContent, OutlinedInputWrapper, ButtonSearch } from "./styled";
import Image from "next/image";

function Status404() {
  return (
    <>
      <MainContent>
        <Container maxWidth="md">
          <Box textAlign="center" sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
            <Image alt="404" height={180} src="/static/images/status/404.svg" />
            <Typography
              variant="h2"
              sx={{
                my: 2,
              }}
            >
              {"The page you were looking for doesn't exist."}
            </Typography>
            <Typography
              variant="h4"
              color="text.secondary"
              fontWeight="normal"
              sx={{
                mb: 4,
              }}
            >
              {"It's on us, we moved the content to a different page. The search below should help!"}
            </Typography>
          </Box>
          <Container maxWidth="sm">
            <Card
              sx={{
                textAlign: "center",
                mt: 3,
                p: 4,
              }}
            >
              <FormControl variant="outlined" fullWidth>
                <OutlinedInputWrapper
                  type="text"
                  placeholder={"Search terms here..."}
                  endAdornment={
                    <InputAdornment position="end">
                      <ButtonSearch variant="contained" size="small">
                        {"Search"}
                      </ButtonSearch>
                    </InputAdornment>
                  }
                  startAdornment={
                    <InputAdornment position="start">
                      <SearchTwoToneIcon />
                    </InputAdornment>
                  }
                />
              </FormControl>
              <Divider
                sx={{
                  my: 4,
                }}
              >
                OR
              </Divider>
              <Button href="/" variant="outlined">
                {"Go to homepage"}
              </Button>
            </Card>
          </Container>
        </Container>
      </MainContent>
    </>
  );
}

export default Status404;
