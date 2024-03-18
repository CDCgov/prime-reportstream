import type { Metadata } from 'next'
 
export const metadata: Metadata = {
  title: process.env.VITE_TITLE,
  description: process.env.VITE_DESCRIPTION,
  themeColor: "#000000",
  openGraph: {
    images: {
        url: process.env.VITE_OPENGRAPH_DEFAULT_IMAGE_SRC ?? "",
        alt: process.env.VITE_OPENGRAPH_DEFAULT_IMAGE_ALTTEXT
    }
  },
  verification: {
    google: "qZbBdujV5kZQv_pCqV2wpfSU25odH35HQukm5ACyLNs"
  }
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
<html lang="en">
    <body>
        <div id="root">{children}</div>
    </body>
</html>
  )
}