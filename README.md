
**TL;DR** An algorithm that randomly generates abstract images of landscapes.

## About Generative Art

My interest in algorithms and blockchain technology, combined with the love for art, led me to explore the topic of generative art. This type of art is made using computer algorithms. Computer algorithms which have been carefully created and fine-tuned in a creative process by the human arist. This has nothing to do with AI-generated art.

> "At the core of the generative process is creative coding. To put it simply, it’s about writing programs that generate artwork." - Tyler Hobbs

Generative art involves creating an algorithm that can make endless unique artworks within certain rules. The art's value comes from the rules and the possibilities set by the algorithm, not each individual piece. Outputs of generative art algorithm usually have some major things in common but differ in the fine details. Each artwork is unique but yet you can tell that they originate from the same algorithm.

Generative art has been around for a while but has become popular again, especially in the crypto space. Platforms like [ArtBlocks](https://www.artblocks.io/) focus on selling generative art. Artists upload their algorithms to these platforms, and the algorithms are stored securely on a blockchain, which means they can't be changed. When someone buys art from these algorithms, the actual artwork is made at the moment of purchase. So, the buyer doesn't know what the artwork looks like until they buy it.

Generative art algorithms need a big random number (called "seed"), to make random decisions during the artwork creation. This starting point comes from different things, like the buyer's blockchain address and the current time. This means that each artwork has unique features tied to the buyer's information, so even if others buy from the same algorithm, the art will look different.

I found inspiration in [Tyler Hobbs](https://tylerxhobbs.com/), an artist from Austin, Texas, who works with generative art. He doesn't just stick to the digital world; he also uses machines to bring his algorithm-generated art to life on paper. Tyler writes a lot about his works, and the tools and processes he uses. His essays inspired me to learn and delve into this rabbit hole.


## About Genscape

After picking up Clojure (the programming language used) and other necessary tools for creating generative art algorithms, I started experimenting. That's when the idea of "Genscape" popped into my mind—an abstract landscape inspired by Gaudi's style. Here's how the Genscape algorithm works:

The algorithm randomly selects a time of day from 0.0 to 24.0. Based on this time, various decisions are made, such as whether the picture will depict day or night, whether there will be a moon or a sun, the height of the sun or moon, the color of the sky, and more. The color of each tile, involves numerous other random variables that contribute to the final decision.

The drawing process begins with the sun or the moon, followed by the sky with a tile pattern featuring rays pointing away from the sun or moon. Once the sky is complete, the foreground is drawn on top. The foreground consists of one to three layers, each having a unique base color and a distinct level of "hilliness." When layered, these create the illusion of a hilly landscape, sometimes with fields, other times with mountains, and occasionally with abstract and colorful elements in between. The scenes vary, representing bright days, sunsets behind mountain peaks, or dark and eerie nights illuminated only by the moon.
