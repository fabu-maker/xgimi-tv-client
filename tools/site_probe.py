#!/usr/bin/env python3
"""External smoke probe. Read only public pages, no credentials or playback extraction."""
from html.parser import HTMLParser
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.watch = set()
        self.images = 0
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        href = attrs.get('href', '')
        if tag == 'a' and (href.startswith('/movie/watch/') or href.startswith('/tv/watch/')):
            self.watch.add(href)
        if tag == 'img':
            self.images += 1

for path in ['/movies', '/tv']:
    try:
        request = Request('https://bingebang.st' + path, headers={'User-Agent':'Mozilla/5.0','Accept':'text/html'})
        with urlopen(request, timeout=15) as response:
            html = response.read(1_800_001)
            if len(html) > 1_800_000:
                raise RuntimeError('page over configured byte cap')
            parser = Links()
            parser.feed(html.decode('utf-8', errors='replace'))
            print(f'{path}: {len(parser.watch)} detail anchors, {parser.images} images, {len(html)} bytes')
            if not parser.watch:
                print('WARNING: no watch links in server-delivered HTML; native catalogue adapter may be unusable')
    except (HTTPError, URLError, RuntimeError) as exc:
        print(f'{path}: public HTTP probe failed ({type(exc).__name__}: {exc})')
